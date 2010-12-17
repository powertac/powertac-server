package org.powertac.server.module.databaseservice.domain;

import org.powertac.server.module.databaseservice.domain.TransactionLog;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.serializable.RooSerializable;

@RooJavaBean
@RooToString
@RooEntity(inheritanceType = "JOINED")
@RooSerializable
public class TradeLog extends TransactionLog {
}
