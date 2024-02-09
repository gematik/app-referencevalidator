/*
 * Copyright (c) 2024 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.refv.cli;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
public class ReferenceValidatorChecker {

    @SneakyThrows
    public static void main(String[] args) {
        if(args.length < 3)
            throw new IllegalStateException("Usage: [absolute-path-to-referencevalidator-jar-with-dependencies] [module] [absolute-path-to-fhir-resource]");
        Process process = getProcess(args);
        boolean isValid = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
                if(line.contains("Valid: true"))
                    isValid = true;
            }
        }

        int exitCode = process.waitFor();

        if(exitCode != 0)
            throw new IllegalStateException("Unexpected RefVal exit code: " + exitCode);

        if(!isValid)
            throw new IllegalStateException("Validation result is false, expected true");
    }

    private static Process getProcess(String[] args) throws IOException {
        String command = String.format("java -jar %s %s %s", args[0], args[1], args[2]);
        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
        processBuilder.redirectErrorStream(true);

        return processBuilder.start();
    }
}
