/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.powertac.server.module.auctioneer;

import org.powertac.common.commands.ShoutChanged;
import org.powertac.common.commands.ShoutCreate;
import org.powertac.common.commands.ShoutDeleteCommand;
import org.powertac.common.commands.ShoutUpdateCommand;
import org.powertac.common.interfaces.Auctioneer;

import java.util.List;

public class AuctioneerImpl implements Auctioneer {

    @Override
    public List processShoutCreate(ShoutCreate shoutCreate) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ShoutChanged processShoutDelete(ShoutDeleteCommand shoutDeleteCommand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ShoutChanged processShoutUpdate(ShoutUpdateCommand shoutUpdateCommand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
