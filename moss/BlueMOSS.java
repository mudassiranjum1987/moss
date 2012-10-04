/*
 *	BlueMOSS.java -- BlueJ-friendly MOSS harness
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

package moss;

import moss.kernel.*;
import moss.user.*;

public class BlueMOSS
{
	//{{{  private attributes
	/** virtual processors */
	MProcessor cpus[];
	/** init-task handle */
	MUserProcess init_task_p;
	//}}}


	//{{{  public BlueMOSS (String bootargs[])
	/**
	 * constructor -- this initialises the MOSS kernel and starts
	 * the init-task
	 *
	 * @param bootargs boot arguments
	 */
	public BlueMOSS (String bootargs[])
	{
		System.out.println ("MOSS (Mini Operating-System Simulator) version " + MConfig.version + " [bluej] starting...");

		/* create virtual processors */
		cpus = new MProcessor[MConfig.ncpus];
		for (int i=0; i<cpus.length; i++) {
			cpus[i] = new MProcessor (i);
		}

		/* start kernel */
		MKernel.init_kernel (cpus, System.out);

		/* create init task */
		init_task_p = new MInitTask ((bootargs == null) ? new String[0] : (String[])(bootargs.clone ()));

		/* this returns immediately -- other threads will be running.. */
	}
	//}}}
	//{{{  public MProcess get_task_list ()
	/**
	 * provides access to the task-list
	 */
	public MProcess get_task_list ()
	{
		return MKernel.task_list;
	}
	//}}}
	
}


