package org.namelessrom.devicecontrol.hardware;

import com.stericson.roottools.RootTools;
import com.stericson.roottools.execution.CommandCapture;
import com.stericson.roottools.execution.Shell;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.Logger;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by alex on 07.10.14.
 */
public class IoSchedulerUtils {

    public static class IoScheduler {
        public final String[] available;
        public final String   current;

        public IoScheduler(final String[] availableIoSchedulers, final String ioScheduler) {
            available = availableIoSchedulers;
            current = ioScheduler;
        }
    }

    public interface IoSchedulerListener {
        public void onIoScheduler(final IoScheduler ioScheduler);
    }

    private static IoSchedulerUtils sInstance;

    private IoSchedulerUtils() { }

    public static IoSchedulerUtils get() {
        if (sInstance == null) {
            sInstance = new IoSchedulerUtils();
        }
        return sInstance;
    }

    /**
     * Gets available schedulers from file
     *
     * @return available schedulers
     */
    public String[] getAvailableIoSchedulers() {
        String[] schedulers = null;
        final String[] aux = Utils.readStringArray(Constants.IO_SCHEDULER_PATH[0]);
        if (aux != null) {
            schedulers = new String[aux.length];
            for (int i = 0; i < aux.length; i++) {
                if (aux[i].charAt(0) == '[') {
                    schedulers[i] = aux[i].substring(1, aux[i].length() - 1);
                } else {
                    schedulers[i] = aux[i];
                }
            }
        }
        return schedulers;
    }

    public void getIoScheduler(final IoSchedulerListener listener) {
        try {
            final Shell mShell = RootTools.getShell(true);
            if (mShell == null) { throw new Exception("Shell is null"); }

            final String cmd = "cat " + Constants.IO_SCHEDULER_PATH[0] + " 2> /dev/null;";

            final StringBuilder outputCollector = new StringBuilder();
            final CommandCapture cmdCapture = new CommandCapture(0, false, cmd) {
                @Override public void commandOutput(int id, String line) {
                    outputCollector.append(line);
                }

                @Override public void commandCompleted(int id, int exitcode) {
                    final List<String> result =
                            Arrays.asList(outputCollector.toString().split(" "));
                    final List<String> tmpList = new ArrayList<String>();
                    String tmpString = "";

                    if (result.size() <= 0) return;

                    for (final String s : result) {
                        if (s.isEmpty()) continue;
                        if (s.charAt(0) == '[') {
                            tmpString = s.substring(1, s.length() - 1);
                            tmpList.add(tmpString);
                        } else {
                            tmpList.add(s);
                        }
                    }

                    final String scheduler = tmpString;
                    final String[] availableSchedulers =
                            tmpList.toArray(new String[tmpList.size()]);
                    Application.HANDLER.post(new Runnable() {
                        @Override public void run() {
                            listener.onIoScheduler(new IoScheduler(availableSchedulers, scheduler));
                        }
                    });

                }
            };

            if (mShell.isClosed()) { throw new Exception("Shell is closed"); }
            mShell.add(cmdCapture);
        } catch (Exception exc) {
            Logger.e(CpuUtils.class, "Error: " + exc.getMessage());
        }
    }
}
