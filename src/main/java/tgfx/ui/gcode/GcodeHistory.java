/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tgfx.ui.gcode;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author rileyporter
 */
public final class GcodeHistory {
    private static final Logger logger = LogManager.getLogger();

    private ArrayList<String> commandHistory = new ArrayList<>();
    private int commandIndex = -1;

    public GcodeHistory() {
        addCommandToHistory("");
    }

    public void addCommandToHistory(String gcl) {
        commandHistory.add(gcl);
        commandIndex++;
    }

    public void clearCommandHistory() {
        commandHistory.clear();
        addCommandToHistory("");
    }

    public String getNextHistoryCommand() {
        String nextHistory;
        if (commandIndex == 0) {
            commandIndex++; //Edge case when you are at the 0th command
            nextHistory = commandHistory.get(commandIndex);
        } else if (commandIndex == commandHistory.size() - 1) {
            nextHistory = commandHistory.get(commandIndex);
        } else {
            commandIndex++;
            nextHistory = commandHistory.get(commandIndex);
        }
        logger.info(" Get Next History got {} at index {}", nextHistory, commandIndex);
        return nextHistory;
    }

    public String getPreviousHistoryCommand() {
        String previousHistory;
        if (commandIndex == commandHistory.size() - 1) {
            commandIndex--; //Edge case when you are at the last command in the history
            previousHistory = commandHistory.get(commandIndex);
        } else if (commandIndex == 0) {
                previousHistory = commandHistory.get(commandIndex);
        } else {
            commandIndex--;
            previousHistory = commandHistory.get(commandIndex);
        }
        logger.info("Get Previous History got {} at index {}", previousHistory, commandIndex);
        return previousHistory;
    }
}
