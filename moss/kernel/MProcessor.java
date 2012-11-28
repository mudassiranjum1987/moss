/*
 *	MProcessor.java -- low-level stuff for abstracting a "processor"
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

import moss.fs.*;

/**
 * This class is used to represent a "virtual processor".
 */

public class MProcessor
{
	//{{{  public and private variables
	/** which CPU, 0 = first */
	public int cpu;

	/** magic */
	private int pmagic = 0xdeadbeef;
	/** <strong>Java</strong> Thred object current running on this "processor".  Used to discover current CPU */
	private Thread current_p = null;


	//}}}
	//{{{  private static class PFS_cpuinfo implements MProcFSIf
	/**
	 * this class provides the "cpuinfo" information for the process file-system
	 */
	private static class PFS_cpuinfo implements MProcFSIf
	{
		/**
		 * called to get the info
		 *
		 * @param inode inode for the proc-fs entry
		 * @param name name of the entry
		 *
		 * @return string containing CPU information
		 */
		public String readproc (MInode inode, String name)
		{
			String r = "";
			int i;

			for (i=0; i<MKernel.processors.length; i++) {
				MProcessor cpu = MKernel.processors[i];

				if (cpu != null) {
					r = r + "CPU" + i + (cpu.is_idle() ? "(idle)" : "(busy)") + ": " + cpu.get_processor_id() + "\n";
				}
			}

			return r;
		}
	}
	//}}}


	//{{{  public MProcessor (int n)
	/**
	 * constructor
	 *
	 * @param n processor number
	 */
	public MProcessor (int n)
	{
		cpu = n;
	}
	//}}}


	//{{{  public static void finalinit ()
	/**
	 * called to do final initialisation (in the context of the init-task)
	 */
	public static void finalinit ()
	{
		MProcFS.register_procimpl ("cpuinfo", new PFS_cpuinfo ());

		return;
	}
	//}}}
	//{{{  public static int currentCPU ()
	/**
	 * returns the virtual CPU number of the currently running process.
	 *
	 * @return processor number
	 */
	public static int currentCPU ()
	{
		Thread t = Thread.currentThread ();

		for (int i=0; i<MKernel.processors.length; i++) {
			if (MKernel.processors[i].pmagic != 0xdeadbeef) {
				MKernel.panic ("MProcessor::currentCPU() corrupt processor");
			}
			if (MKernel.processors[i].current_p == t) {
				return i;
			}
		}
		MKernel.panic ("MProcessor::currentCPU() current thread not running on any processor");
		return -1;
	}
	public static int currentCPU (Thread t)
	{
		for (int i=0; i<MKernel.processors.length; i++) {
			if (MKernel.processors[i].pmagic != 0xdeadbeef) {
				MKernel.panic ("MProcessor::currentCPU() corrupt processor");
			}
			if (MKernel.processors[i].current_p == t) {
				return i;
			}
		}
		MKernel.panic ("MProcessor::currentCPU() current thread not running on any processor");
		return -1;
	}
	//}}}
	//{{{  public synchronized void set_process (MProcess current)
	/**
	 * sets the currently executing process of this processor
	 *
	 * @param current process now running on this processor.
	 * 		if null, means the processor is going idle.
	 */
	public synchronized void set_process (MProcess current)
	{
		current_p = current;
	}
	//}}}
	//{{{  public synchronized boolean is_idle ()
	/**
	 * tests whether the processor is idle or not
	 *
	 * @return true if idle, false if active
	 */
	public synchronized boolean is_idle ()
	{
		return (current_p == null);
	}
	//}}}
	//{{{  public synchronized String get_processor_id ()
	/**
	 * returns the "processor ID" string
	 *
	 * @return processor ID string
	 */
	public synchronized String get_processor_id ()
	{
		return "VCPU:JVM/" + System.getProperty ("java.vm.version") + "/" + System.getProperty("os.name") + "/" + System.getProperty("os.version");
	}
	//}}}
}


