/*
 *	MModules.java -- kernel-module helper
 *	Copyright (C) 2004 Fred Barnes <frmb@kent.ac.uk>
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 2 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program; if not, write to the Free Software
 *	Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package moss.kernel;

import moss.user.*;
import moss.fs.*;

import java.util.*;

/**
 * this class provides some registration services for kernel-modules,
 * prevents multiple build-ups of the same module
 */

public class MModules
{
	//{{{  private stuff
	private static Object synclock = null;		/* used for protecting the vars below */
	private static Hashtable regmodules = null;
	private static ArrayList a_regmodules = null;

	//{{{  private static class PFS_modules implements MProcFSIf
	/**
	 * this class provides the /proc/modules entry
	 */
	private static class PFS_modules implements MProcFSIf
	{
		public String readproc (MInode inode, String name)
		{
			String r = "";
			int i;

			synchronized (synclock) {
				for (i=0; i<a_regmodules.size(); i++) {
					String s = (String)(a_regmodules.get (i));

					r += s + "\n";
				}
			}

			return r;
		}
	}
	//}}}

	//}}}


	//{{{  public static void init_modules ()
	/**
	 * called to initialise the kernel-module handler
	 */
	public static void init_modules ()
	{
		regmodules = new Hashtable ();
		a_regmodules = new ArrayList ();
		synclock = new Object ();
		
		return;
	}
	//}}}
	//{{{  public static void finalinit ()
	/**
	 * finalising init for the kernel-module handler
	 */
	public static void finalinit ()
	{
		MProcFS.register_procimpl ("modules", new PFS_modules ());

		return;
	}
	//}}}
	//{{{  public static void shutdown_modules ()
	/**
	 * called to shut-down the kernel-module handler
	 */
	public static void shutdown_modules ()
	{
		return;
	}
	//}}}


	//{{{  public static int register_module (String mname)
	/**
	 * registers a module
	 *
	 * @param mname module name
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int register_module (String mname)
	{
		synchronized (synclock) {
			if (regmodules.containsKey (mname)) {
				return -MSystem.EEXISTS;
			}
			regmodules.put (mname, mname);
			a_regmodules.add (mname);
		}

		return 0;
	}
	//}}}
	//{{{  public static void unregister_module (String mname)
	/**
	 * unregisters a module
	 *
	 * @param mname module name
	 */
	public static void unregister_module (String mname)
	{
		int idx;

		synchronized (synclock) {
			regmodules.remove (mname);
			idx = a_regmodules.indexOf (mname);
			if (idx >= 0) {
				a_regmodules.remove (idx);
			}
		}

		return;
	}
	//}}}
}
