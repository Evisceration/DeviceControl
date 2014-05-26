/*
 * This file is part of the RootTools Project: http://code.google.com/p/roottools/
 *
 * Copyright (c) 2012 Stephen Erickson, Chris Ravenscroft, Dominik Schuermann, Adam Shanks
 *
 * This code is dual-licensed under the terms of the Apache License Version 2.0 and
 * the terms of the General Public License (GPL) Version 2.
 * You may use this code according to either of these licenses as is most appropriate
 * for your project on a case-by-case basis.
 *
 * The terms of each license can be found in the root directory of this project's repository as
 * well as at:
 *
 * * http://www.apache.org/licenses/LICENSE-2.0
 * * http://www.gnu.org/licenses/gpl-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under these Licenses is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See each License for the specific language governing permissions and
 * limitations under that License.
 */

package com.stericson.roottools.execution;

import android.content.Context;

import com.stericson.roottools.RootTools;

public class JavaCommandCapture extends Command {
    private final StringBuilder sb = new StringBuilder();

    public JavaCommandCapture(final int id, final Context context, final String... command) {
        super(id, true, context, command);
    }

    public JavaCommandCapture(final int id, final boolean handlerEnabled, final Context context,
            final String... command) {
        super(id, handlerEnabled, true, context, command);
    }

    public JavaCommandCapture(final int id, final int timeout, final Context context,
            final String... command) {
        super(id, timeout, true, context, command);
    }

    @Override
    public void commandOutput(final int id, final String line) {
        sb.append(line).append('\n');
        RootTools.log("Command", "ID: " + id + ", " + line);
    }

    @Override
    public void commandTerminated(final int id, final String reason) {
        // pass
    }

    @Override
    public void commandCompleted(final int id, final int exitCode) {
        // pass
    }

    @Override
    public String toString() { return sb.toString(); }

}
