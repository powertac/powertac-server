package org.powertac.common.enumerations;

/**
 * Describes a set of possible reasons
 * for changing a shout. The codes are
 * chosen to be in line with the official
 * Xetra stock exchange codes
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 */
public enum ModReasonCode {
    INSERT(1), // 001
    MODIFICATION(2),// 002
    DELETIONBYUSER(3), // 003
    EXECUTION(4), // 004
    PARTIALEXECUTION(5), // 005
    DELETIONBYSYSTEM(6); // 006

    private final int idVal;

    ModReasonCode(int id) {this.idVal = id; }
    public int getId() { return idVal; }
}