/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.config.persist.repository.local;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.infra.config.persist.repository.DistMetaDataPersistRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Local dist meta data persist repository.
 */
@Slf4j
public final class LocalDistMetaDataPersistRepository implements DistMetaDataPersistRepository {
    
    private String path;
    
    @Override
    public String get(final String key) {
        if (!Files.exists(Paths.get(path, key))) {
            return "";
        }
        try {
            return Files.readAllLines(Paths.get(path, key)).stream().map(each -> each + System.lineSeparator()).collect(Collectors.joining());
        } catch (final IOException ex) {
            //TODO process exception
            log.error("Get local dist meta data by key: {} failed", key, ex);
        }
        return "";
    }
    
    @Override
    public List<String> getChildrenKeys(final String key) {
        return Arrays.stream(new File(path, key).listFiles()).map(File::getName).collect(Collectors.toList());
    }
    
    @Override
    public void persist(final String key, final String value) {
        File file = new File(path, key);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(path, key))) {
            bufferedWriter.write(value);
            bufferedWriter.flush();
        } catch (final IOException ex) {
            //TODO process exception
            log.error("Persist local dist meta data to key: {} failed", key, ex);
        }
    }
    
    @Override
    public void delete(final String key) {
        boolean isDeleted = new File(path, key).delete();
        if (!isDeleted) {
            //TODO process exception
            log.error("Delete local dist meta data key: {} failed", key);
        }
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public String getType() {
        return "Local";
    }
    
    @Override
    public void setProps(final Properties props) {
        LocalRepositoryProperties localRepositoryProperties = new LocalRepositoryProperties(props);
        path = Optional.ofNullable(Strings.emptyToNull(localRepositoryProperties.getValue(LocalRepositoryPropertyKey.PATH)))
                .orElse(System.getProperty("user.dir"));
    }
}
