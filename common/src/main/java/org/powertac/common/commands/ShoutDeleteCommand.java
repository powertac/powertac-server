package org.powertac.common.commands;

/**
 * Command object that can be used by
 * a broker to require the server to delete
 * as specific shout;
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 */
public class ShoutDeleteCommand {
  String authToken;
  Long shoutId;
}
