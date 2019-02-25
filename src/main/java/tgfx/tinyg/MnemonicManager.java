package tgfx.tinyg;

import static tgfx.tinyg.MnemonicConstants.*;

/**
 * MnemonicManager
 * Retrieves Mnemonics
 */
public class MnemonicManager {

    public MnemonicManager() {
    }

    public boolean isMasterGroupObject(String strToLookup) {
        return GROUP_MNEMONICS.contains(strToLookup);
    }

    public ResponseCommand lookupSingleGroupMaster(String strToLookup, String parentGroup) {
        // This will iterate all group mnemonics to see if the single group object
        // belongs in which group.
        ResponseCommand rc = new ResponseCommand(parentGroup, null, null);

        if (AXIS_MNEMONICS.contains(strToLookup)) {
            rc.setSettingKey(strToLookup);
        } else if (MOTOR_MNEMONICS.contains(strToLookup)) {
            rc.setSettingKey(strToLookup);
        } else if (SYS_MNEMONICS.contains(strToLookup)) {
            rc.setSettingKey(strToLookup);
        } else if (STATUS_MNEMONICS.contains(strToLookup)) {
            rc.setSettingKey(strToLookup);
        } else {
            return null;
        }
        return rc;
    }

    public ResponseCommand lookupSingleGroup(String strToLookup) {
        // This will iterate all group mnemonics to see if the single group object
        // belongs in which group.
        ResponseCommand rc = new ResponseCommand();

        if (AXIS_MNEMONICS.contains(strToLookup.substring(1))) {
            rc.setSettingParent(String.valueOf(strToLookup.charAt(0)));
            rc.setSettingKey(strToLookup.substring(1));
        } else if (MOTOR_MNEMONICS.contains(strToLookup.substring(1))) {
            rc.setSettingParent(String.valueOf(strToLookup.charAt(0)));
            rc.setSettingKey(strToLookup.substring(1));
        } else if (SYS_MNEMONICS.contains(strToLookup)) {
            rc.setSettingParent(MNEMONIC_GROUP_SYSTEM);
            rc.setSettingKey(strToLookup);
        } else if (STATUS_MNEMONICS.contains(strToLookup)) {
            rc.setSettingParent(MNEMONIC_GROUP_STATUS_REPORT);
            rc.setSettingKey(strToLookup);
        } else {
            return null;
        }
        return rc;
    }
}
