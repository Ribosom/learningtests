/*
 * Copyright © 2017-2020 factcast.org
 *
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
 */
package org.samba;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.postgresql.Driver;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@ExtendWith({FactCastExtension.class})
@Slf4j
public class AbstractFactCastIntegrationTest {

    protected static final Network _docker_network = Network.newNetwork();

    @Container
    protected static final PostgreSQLContainer _postgres =
            new PostgreSQLContainer<>("postgres:11.5")
                    .withDatabaseName("fc")
                    .withUsername("fc")
                    .withPassword("fc")
                    .withNetworkAliases("db")
                    .withNetwork(_docker_network);

    @Container
    protected static final GenericContainer _factcast =
            new GenericContainer<>("factcast/factcast:latest")
                    .withExposedPorts(9090)
                    .withFileSystemBind("./config", "/config/")
                    .withEnv("grpc_server_port", "9090")
                    .withEnv("factcast_security_enabled", "false")
                    .withEnv("spring_datasource_url", "jdbc:postgresql://db/fc?user=fc&password=fc")
                    .withNetwork(_docker_network)
                    .dependsOn(_postgres)
                    .withLogConsumer(new Slf4jLogConsumer(log))
                    .waitingFor(new HostPortWaitStrategy().withStartupTimeout(Duration.ofSeconds(180)));

    @SuppressWarnings("rawtypes")
    @Container
    static final GenericContainer _redis =
            new GenericContainer<>("redis:5.0.9-alpine").withExposedPorts(6379);

    @BeforeAll
    public static void startContainers() throws InterruptedException {
        String url = _postgres.getJdbcUrl();
        // TODO geht ni
        System.setProperty("spring.datasource.driver-class-name", Driver.class.getName());
        System.setProperty("spring.datasource.type", "com.zaxxer.hikari.HikariDataSource");
        System.setProperty("spring.datasource.url", url);
        System.setProperty("spring.datasource.username", _postgres.getUsername());
        System.setProperty("spring.datasource.password", _postgres.getPassword());

        String address = "static://" + _factcast.getHost() + ":" + _factcast.getMappedPort(9090);
        System.setProperty("grpc.client.factstore.address", address);

        System.setProperty("spring.redis.host", _redis.getHost());
        System.setProperty("spring.redis.port", String.valueOf(_redis.getMappedPort(6379)));
    }
}
