/*
 *	MiniOSSim.java -- harness for starting the simulator
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

import javax.xml.bind.annotation.XmlElementDecl.GLOBAL;

import moss.kernel.*;
import moss.memory.*;
import moss.user.*;
import moss.Time.*;

public class MiniOSSim
{
	public static void main (String args[])
	{
		MProcessor cpus[];
		
		//memoryFrame memory = new memoryFrame();
		
		MProcessTiming.initializeMProcessSchedules();
		
		System.out.println ("MOSS (Mini Operating-System Simulator) version " + MConfig.version + " starting...");

		/* initialise CPU objects */
		cpus = new MProcessor[MConfig.ncpus];
		for (int i = 0; i < cpus.length; i++) {
			cpus[i] = new MProcessor (i);
		}

		/* initialise kernel */
		MKernel.init_kernel (cpus, System.out);

		//BatchProcessCreationExamples.batchProcessCreationExample1();
		
		/* create init task */
		new MInitTask ((String[])(args.clone ()));

		/* then sleep forever */
		Object o = new Object ();
		synchronized (o) {
			try {
				o.wait ();
			} catch (InterruptedException e) {
				/* do nothing */
			}
		}
	}
}

