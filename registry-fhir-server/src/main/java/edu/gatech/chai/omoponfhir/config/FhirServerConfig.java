/*******************************************************************************
 * Copyright (c) 2019 Georgia Tech Research Institute
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
 *******************************************************************************/
package edu.gatech.chai.omoponfhir.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import edu.gatech.chai.omopv5.dba.config.DatabaseConfigurationImpl;
import edu.gatech.chai.omopv5.dba.config.DatabaseConfiguration;

@Configuration
@EnableScheduling
@EnableTransactionManagement
@ComponentScans(value = { 
	@ComponentScan("edu.gatech.chai.omopv5.dba.config"),
	@ComponentScan("edu.gatech.chai.omopv5.dba.service"),
	@ComponentScan("edu.gatech.chai.omoponfhir.local.task"),
	@ComponentScan("edu.gatech.chai.omoponfhir.omopv5.r4.provider"),
	@ComponentScan("edu.gatech.chai.omoponfhir.config"),
	@ComponentScan("edu.gatech.chai.omoponfhir.omopv5.r4.utilities"),
	@ComponentScan("edu.gatech.chai.omoponfhir.r4.security")})
@ImportResource({
    "classpath:database-config.xml"
})
public class FhirServerConfig {
	@Autowired
	DataSource dataSource;

	@Value("${server.baseurl}")
    private String serverBaseUrl;

	@Bean()
	public DatabaseConfiguration databaseConfiguration() {
		DatabaseConfigurationImpl databaseConfiguration = new DatabaseConfigurationImpl();

		// What driver do we want to use?
		String targetDatabase = System.getenv("TARGETDATABASE");
		databaseConfiguration.setSqlRenderTargetDialect(targetDatabase);

		if ("bigquery".equalsIgnoreCase(targetDatabase)) {
			databaseConfiguration.setBigQueryDataset(System.getenv("BIGQUERYDATASET"));
			databaseConfiguration.setBigQueryProject(System.getenv("BIGQUERYPROJECT"));
		} else {
			databaseConfiguration.setDataSource(dataSource);
			if (targetDatabase == null || targetDatabase.isEmpty())
				databaseConfiguration.setSqlRenderTargetDialect("postgresql");
		}
		
		return databaseConfiguration;
	}

	public String getServerBaseUrl() {
		return this.serverBaseUrl;
	}
}
