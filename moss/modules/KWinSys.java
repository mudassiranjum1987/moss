/*
 *	KWinSys.java -- windowing system module for MOSS
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

package moss.modules;

import moss.kernel.*;
import moss.user.*;
import moss.ipc.*;

import javax.swing.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;


public class KWinSys implements MKernelProcess
{
	//{{{  private stuff
	private JFrame window;
	private Canvas canvas;
	private int width;
	private int height;

	//}}}
	//{{{  public stuff
	public static final int EVENT_WINDOW = 0;
	public static final int EVENT_MOUSE = 1;
	public static final int EVENT_KEY = 2;

	//}}}


	//{{{  public class KWSEvent
	/**
	 * this class is used to communicate actions between the various
	 * listeners (presumably running in some event thread), and the main
	 * KWinSys process (that sits on recvmsg())
	 */
	public class KWSEvent
	{
		public int type;
		public Object data;


		public KWSEvent (int type, Object data)
		{
			this.type = type;
			this.data = data;
		}
	}
	//}}}
	//{{{  private class KWinSysWindowListener extends WindowAdapter
	/**
	 * this class provides the action-listener for the top-level
	 */
	public class KWinSysWindowListener extends WindowAdapter
	{
		/** process ID of the KWinSys module */
		private int pid;

		//{{{  public KWinSysWindowListener (int pid)
		/**
		 * constructor
		 *
		 * @param pid process ID of the KWinSys module
		 */
		public KWinSysWindowListener (int pid)
		{
			this.pid = pid;
		}
		//}}}
		//{{{  public void windowClosing (WindowEvent e)
		public void windowClosing (WindowEvent e)
		{
			MMailBox.sendmsg (pid, pid, 0, new KWSEvent (KWinSys.EVENT_WINDOW, e));
			return;
		}
		//}}}
	}
	//}}}
	//{{{  public class KWinSysMouseListener extends MouseAdapter
	/**
	 * this class provides a mouse-listener for the top-level
	 */
	public class KWinSysMouseListener extends MouseAdapter
	{
		/** process ID of the KWinSys module */
		private int pid;

		//{{{  public KWinSysMouseListener (int pid)
		/**
		 * constructor
		 *
		 * @param pid process ID of the KWinSys module
		 */
		public KWinSysMouseListener (int pid)
		{
			this.pid = pid;
		}
		//}}}
		//{{{  public void mouseClicked (MouseEvent e)
		public void mouseClicked (MouseEvent e)
		{
			/* accesses MMailBox directly -- don't have a sensible process context.. */
			MMailBox.sendmsg (pid, pid, 0, new KWSEvent (KWinSys.EVENT_MOUSE, e));
			return;
		}
		//}}}
	}
	//}}}
	//{{{  public class KWinSysKeyListener extends KeyAdapter
	/**
	 * this class provides a key-listener for the top-level
	 */
	public class KWinSysKeyListener extends KeyAdapter
	{
		/** process ID of the KWinSys module */
		private int pid;

		//{{{  public KWinSysKeyListener (int pid)
		/**
		 * constructor
		 *
		 * @param pid process ID of the KWinSys module
		 */
		public KWinSysKeyListener (int pid)
		{
			this.pid = pid;
		}
		//}}}
		//{{{  public void keyTyped (KeyEvent e)
		public void keyTyped (KeyEvent e)
		{
			/* accesses MMailBox directly -- don't have a sensible process context.. */
			MMailBox.sendmsg (pid, pid, 0, new KWSEvent (KWinSys.EVENT_KEY, e));
			return;
		}
		//}}}
	}
	//}}}
	//{{{  public class KWinSysCanvas extends Canvas
	/**
	 * this class extends a canvas and deals with the drawing operation
	 */
	public class KWinSysCanvas extends Canvas
	{
		public void paint (Graphics g)
		{
			g.drawString ("foobar 20 20", 20, 20);
			g.drawString ("foobar 50 100", 50, 100);
			return;
		}
	}
	//}}}

	//{{{  public int main (String argv[])
	/**
	 * entry-point for the kernel module
	 *
	 * @param argv arguments: module name and any options
	 */
	public int main (String argv[])
	{
		int pid = MPosixIf.getpid ();
		boolean running = true;
		WindowListener w_lstn;
		MouseListener m_lstn;
		KeyListener k_lstn;

		MKernel.log_msg (argv[0] + ": module loaded and active!  path is [" + argv[1] + "]");

		width = 640;
		height = 480;

		canvas = new KWinSysCanvas ();
		window = new JFrame ("MOSS " + argv[0] + " [" + argv[1] + "]");
		window.getContentPane().add (canvas);
		canvas.setSize (new Dimension (width, height));
		window.setSize (new Dimension (width, height));
		window.setResizable (false);
		w_lstn = new KWinSysWindowListener (pid);
		m_lstn = new KWinSysMouseListener (pid);
		k_lstn = new KWinSysKeyListener (pid);
		window.addWindowListener (w_lstn);
		canvas.addMouseListener (m_lstn);
		canvas.addKeyListener (k_lstn);
		canvas.setBackground (new Color (0xe0e0ff));
		canvas.setForeground (Color.BLACK);
		canvas.setFont (new Font ("fixed", Font.PLAIN, 12));

		window.setVisible (true);
		window.pack ();

		while (running) {
			Object msg;

			msg = MPosixIf.recvmsg (pid, -1);
			if (msg == null) {
				MKernel.log_msg (argv[0] + ": interrupted ?");
			} else {
				KWSEvent ev = (KWSEvent)msg;

				switch (ev.type) {
				case EVENT_WINDOW:
					{
						WindowEvent e = (WindowEvent)ev.data;

						if (e.getID() == WindowEvent.WINDOW_CLOSING) {
							MKernel.log_msg (argv[0] + ": shutting down");
							running = false;
						}
					}
					break;
				case EVENT_MOUSE:
					{
						MouseEvent e = (MouseEvent)ev.data;
						int x = e.getX ();
						int y = e.getY ();

						canvas.getGraphics().drawString ("X", x, y);
					}
					break;
				case EVENT_KEY:
					{
						KeyEvent e = (KeyEvent)ev.data;

					}
					break;
				default:
					MKernel.log_msg (argv[0] + ": got message type " + ev.type);
					break;
				}
			}
		}

		/* clean-up */
		window.setVisible (false);
		canvas.removeKeyListener (k_lstn);
		canvas.removeMouseListener (m_lstn);
		window.removeWindowListener (w_lstn);
		window.getContentPane().remove (canvas);
		canvas = null;
		window = null;

		return 0;
	}
	//}}}

}

