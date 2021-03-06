/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.hadoop.ozone.admin.scm;

import java.io.IOException;
import java.util.List;
import org.apache.hadoop.hdds.cli.HddsVersionProvider;
import org.apache.hadoop.hdds.scm.cli.ScmSubcommand;
import org.apache.hadoop.hdds.scm.client.ScmClient;
import picocli.CommandLine;

/**
 * Handler of scm status command.
 */
@CommandLine.Command(
    name = "roles",
    description = "List all SCMs, their respective Ratis server roles " +
        "and RaftPeerIds",
    mixinStandardHelpOptions = true,
    versionProvider = HddsVersionProvider.class)
public class GetScmRatisRolesSubcommand extends ScmSubcommand {

  @CommandLine.ParentCommand
  private ScmAdmin parent;

  @Override
  protected void execute(ScmClient scmClient) throws IOException {
    List<String> roles = scmClient.getScmRatisRoles();
    for (String role: roles) {
      System.out.println(role);
    }
  }
}
