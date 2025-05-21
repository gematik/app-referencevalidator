/*-
 * #%L
 * valmodule-erpta7
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * *******
 * 
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */
package de.gematik.refv.valmodule.erpta7;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.IntegratedValidationModule;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.commons.validation.ValidationResult;
import de.gematik.refv.valmodule.erpta7.helper.BundleReducer;
import de.gematik.refv.valmodule.erpta7.helper.DocumentParser;
import de.gematik.refv.valmodule.erpta7.helper.DuplicateChecker;
import de.gematik.refv.valmodule.erpta7.helper.NonReachableEntriesChecker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;

@Slf4j
public class ErpTa7RechnungBundleValidator {
    private final DuplicateChecker duplicateChecker = new DuplicateChecker();
    private final NonReachableEntriesChecker nonReachableEntriesChecker = new NonReachableEntriesChecker();
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final ValidationModule validator;
    private final DocumentParser documentParser = new DocumentParser();

    @SneakyThrows
    public ErpTa7RechnungBundleValidator() {
        validator = getErpValidationModule();
    }

    @SneakyThrows
    public ValidationResult validateBundleConcurrently(String resourceBody, ValidationOptions options) {
        Document document = documentParser.getDocument(resourceBody);

        var nonReachableEntriesValidationMessages = nonReachableEntriesChecker.checkForNonReachableEntries(document);

        BundleReducer bundleReducer = new BundleReducer(document);
        String reducedResourceBody = bundleReducer.getReducedResourceBody();
        log.debug("Extracting GKVSV_PR_TA7_RezeptBundle entries...");
        List<String> allRezeptBundlesAsString = bundleReducer.getAllRezeptBundlesAsString();
        log.debug("Validating reduced TA7-Bundle...");
        var reducedBodyValidationMessages = validator.validateString(reducedResourceBody, options).getValidationMessages();
        var concurrentValidationMessages = performConcurrentValidation(allRezeptBundlesAsString, options).stream().map(ValidationResult::getValidationMessages);
        log.debug("Checking for duplicate fullURLs...");
        var duplicateFullUrlsMessages = duplicateChecker.findDuplicateFullUrls(resourceBody);

        var allValidationMessages = new LinkedList<SingleValidationMessage>();
        Stream.concat(
                concurrentValidationMessages,
                Stream.of(
                        nonReachableEntriesValidationMessages,
                        reducedBodyValidationMessages,
                        duplicateFullUrlsMessages
                )
        ).forEach(allValidationMessages::addAll);

        return new ValidationResult(allValidationMessages);
    }

    private Collection<ValidationResult> performConcurrentValidation(List<String> allEntriesAsStrings, ValidationOptions options) {
        var validationResults = new LinkedList<ValidationResult>();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<ValidationResult>> futures = new ArrayList<>();

        // Submit each string validation task to the ExecutorService
        for (String entryString : allEntriesAsStrings) {
            log.debug("Validating GKVSV_PR_TA7_RezeptBundle #{}...", futures.size()+1);
            Callable<ValidationResult> task = () -> validator.validateString(entryString, options);
            Future<ValidationResult> future = executor.submit(task);
            futures.add(future);
        }

        // Wait for all tasks to be finished
        for (Future<ValidationResult> future : futures) {
            try {
                ValidationResult futureResult = future.get();
                validationResults.add(futureResult);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error occurred while waiting for task to complete: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        try {
            executor.shutdown();
            // Wait for all tasks to be terminated
            boolean terminated = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (!terminated) {
                log.error("ExecutorService did not terminate within the specified time frame");
            }
        } catch (InterruptedException e) {
            log.error("Error occurred while waiting for all tasks to complete: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }

        return validationResults;
    }

    private ValidationModule getErpValidationModule() {
        GenericValidator engine = new GenericValidator(FhirContext.forR4());
        return new IntegratedValidationModule("erp", engine);
    }
}
