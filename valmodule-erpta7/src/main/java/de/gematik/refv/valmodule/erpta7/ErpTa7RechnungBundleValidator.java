/*
Copyright (c) 2022-2024 gematik GmbH

Licensed under the Apache License, Version 2.0 (the License);
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an 'AS IS' BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package de.gematik.refv.valmodule.erpta7;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.SingleValidationMessage;
import de.gematik.refv.commons.validation.GenericValidator;
import de.gematik.refv.commons.validation.IntegratedValidationModule;
import de.gematik.refv.commons.validation.ValidationModule;
import de.gematik.refv.commons.validation.ValidationOptions;
import de.gematik.refv.commons.validation.ValidationResult;
import de.gematik.refv.valmodule.erpta7.helper.DuplicateChecker;
import de.gematik.refv.valmodule.erpta7.helper.BundleReducer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ErpTa7RechnungBundleValidator {

    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final ValidationModule validator;

    private final DuplicateChecker duplicateChecker = new DuplicateChecker();

    @SneakyThrows
    public ErpTa7RechnungBundleValidator() {
        validator = getErpValidationModule();
    }

    @SneakyThrows
    public ValidationResult validateBundleConcurrently(String resourceBody, ValidationOptions options) {
        BundleReducer bundleReducer = new BundleReducer(resourceBody);
        String reducedResourceBody = bundleReducer.getReducedResourceBody();
        List<String> allRezeptBundlesAsString = bundleReducer.getAllRezeptBundlesAsString();
        ValidationResult result = validator.validateString(reducedResourceBody, options);
        performConcurrentValidation(allRezeptBundlesAsString, result, options);
        result.getValidationMessages().addAll(checkFullUrlUniqueness(resourceBody));
        return result;
    }

    private List<SingleValidationMessage> checkFullUrlUniqueness(String resourceBody) {
        var result = new LinkedList<SingleValidationMessage>();
        result.addAll(duplicateChecker.findDuplicateFullUrls(resourceBody));
        result.addAll(duplicateChecker.findDuplicateCompositionReferences(resourceBody));
        return result;
    }

    private void performConcurrentValidation(List<String> allEntriesAsStrings, ValidationResult result, ValidationOptions options) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<ValidationResult>> futures = new ArrayList<>();

        // Submit each string validation task to the ExecutorService
        for (String entryString : allEntriesAsStrings) {
            Callable<ValidationResult> task = () -> validator.validateString(entryString, options);
            Future<ValidationResult> future = executor.submit(task);
            futures.add(future);
        }

        // Wait for all tasks to be finished
        for (Future<ValidationResult> future : futures) {
            try {
                ValidationResult futureResult = future.get();
                result.getValidationMessages().addAll(futureResult.getValidationMessages());
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

    }

    private ValidationModule getErpValidationModule() {
        GenericValidator engine = new GenericValidator(FhirContext.forR4());
        return new IntegratedValidationModule("erp", engine);
    }
}
