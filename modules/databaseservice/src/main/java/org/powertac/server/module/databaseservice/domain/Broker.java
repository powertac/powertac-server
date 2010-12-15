package org.powertac.server.module.databaseservice.domain;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.serializable.RooSerializable;

@RooJavaBean
@RooToString
@RooEntity
@RooSerializable
public class Broker {

    private String authToken;
}
