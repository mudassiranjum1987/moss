/*
 *	MPosixIf.java -- POSIX-ish interface for MOSS
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

package moss.user;

import moss.fs.*;
import moss.kernel.*;
import moss.kernel.Scheduler.ProcessPriorityEnum;
import moss.drivers.*;
import moss.ipc.*;
import moss.net.*;

/**
 * the MPosixIF interface provides the glue between applications and
 * the kernel.
 */

public class MPosixIf
{
	//{{{  constants
	/** standard input descriptor */
	public static final int STDIN = 0;
	/** standard output descriptor */
	public static final int STDOUT = 1;
	/** standard error descriptor */
	public static final int STDERR = 2;
	//}}}

	//{{{  public static int pipe (int fds[])
	/**
	 * This method creates a new pair of pipe descriptors (and the pipe).
	 * The current implementation of the pipe is symmetric -- a process can fill
	 * the pipe and read it back all on one descriptor (not serious, though).
	 *
	 * @param fds array where returned descriptors are stored.  fds[0] is for reading,
	 * 		fds[1] for writing.
	 *
	 * @return on success, 0 is returned, otherwise &lt; 0 indicating error.
	 */
	public static int pipe (int fds[])
	{
		/* make sure this process has room */
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int i, j;

		for (i = 0, j = -1; i < current.files.length; i++) {
			if (current.files[i] == null) {
				if (j > -1) {
					break;
				} else {
					j = i;
				}
			}
		}
		if (i == current.files.length) {
			MProcess.sync_process_signals (current);
			return -MSystem.EMFILE;
		}
		/* j has one free slot, i has the other */

		current.syscall = "pipe";

		MPipe tp = new MPipe ();
		MFile fhr = new MFile ();
		MFile fhw = new MFile ();

		fhr.refcount = 1;
		fhr.fileif = tp;
		fhr.dirif = null;
		fhw.refcount = 1;
		fhw.fileif = tp;
		fhw.dirif = null;

		/* open the pipe (artificial!) */
		tp.open (fhr, MFileOps.OPEN_READ);
		tp.open (fhw, MFileOps.OPEN_WRITE);
		current.files[i] = fhr;
		current.files[j] = fhw;
		fds[0] = i;
		fds[1] = j;
		MProcess.sync_process_signals (current);

		current.syscall = null;
		return 0;
	}
	//}}}
	//{{{  public static int open_device (String name, int flags)
	/**
	 * this opens a device by name (until we get a file-system..)
	 *
	 * @param name name of device to open
	 * @param flags open flags
	 *
	 * @return file-handle on success, otherwise -ve value indicating error
	 */
	public static int open_device (String name, int flags)
	{
		/* make sure this process has room */
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int i, v;
		MFile fh;
		int drvmajor;

		for (i = 0; i < current.files.length; i++) {
			if (current.files[i] == null) {
				break;
			}
		}
		if (i == current.files.length) {
			MProcess.sync_process_signals (current);
			return -MSystem.EMFILE;
		}
		/* i has free slot index */

		drvmajor = MDevices.find_driver (name);
		if (drvmajor < 0) {
			return -MSystem.ENODEV;
		}

		current.syscall = "open_device";

		fh = new MFile ();
		fh.refcount = 1;
		fh.pdata = null;
		fh.offset = 0;
		fh.fileif = null;
		fh.dirif = null;
		fh.inode = new MInode ();
		fh.inode.major = drvmajor;
		fh.inode.minor = 0;
		fh.inode.ino = 0;

		v = MDevices.open (fh, fh.inode.major, fh.inode.minor, flags);
		if (v < 0) {
			current.syscall = null;
			return v;
		}
		current.files[i] = fh;
		MProcess.sync_process_signals (current);

		current.syscall = null;
		return i;
	}
	//}}}
	//{{{  public static int open (String path, int flags)
	/**
	 * This method is used to open a file
	 *
	 * @param path path to file
	 * @param flags open flags (MFileOps.OPEN_...)
	 *
	 * @return file-descriptor on success, otherwise &lt; 0 indicating error
	 */
	public static int open (String path, int flags)
	{
		return open (path, flags, 0644);
	}
	//}}}
	//{{{  public static int open (String path, int flags, int mode)
	/**
	 * This method is used to open a file, with a file-mode if needed
	 *
	 * @param path path to file
	 * @param flags open flags (MFileOps.OPEN_...)
	 * @param mode mode if creating file
	 *
	 * @return file-descriptor on success, otherwise &lt; 0 indicating error
	 */
	public static int open (String path, int flags, int mode)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int fd, i;
		MFile fh;

		MProcess.sync_process_signals (current);
		
		for (fd = 0; fd < current.files.length; fd++) {
			if (current.files[fd] == null) {
				break;
			}
		}
		if (fd == current.files.length) {
			MProcess.sync_process_signals (current);
			return -MSystem.EMFILE;
		}

		current.syscall = "open3";

		fh = new MFile ();
		fh.refcount = 1;
		fh.pdata = null;
		fh.offset = 0;
		fh.fileif = null;
		fh.dirif = null;

		mode &= ~current.umask;
		/* remove any trailing slash from `path', if not just "/" */
		if ((path.length() > 1) && (path.charAt(path.length() - 1) == '/')) {
			i = MFileSystem.open (path.substring (0, path.length() - 1), fh, flags, mode);
		} else {
			i = MFileSystem.open (path, fh, flags, mode);
		}
		if (i < 0) {
			/* failed to open */
			fh.refcount = 0;
			current.syscall = null;
			return i;
		} else {
			current.files[fd] = fh;
		}

		current.syscall = null;
		return fd;
	}
	//}}}
	//{{{  public static int opendir (String path)
	/**
	 * This method is used to open a directory
	 *
	 * @param path path to directory
	 *
	 * @return file-descriptor on success, otherwise &lt; 0 indicating error
	 */
	public static int opendir (String path)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MFile fh;
		int fd, i;

		for (fd = 0; fd < current.files.length; fd++) {
			if (current.files[fd] == null) {
				break;
			}
		}
		if (fd == current.files.length) {
			MProcess.sync_process_signals (current);
			return -MSystem.EMFILE;
		}

		current.syscall = "opendir";

		fh = new MFile ();
		fh.refcount = 1;
		fh.pdata = null;
		fh.offset = 0;
		fh.fileif = null;
		fh.dirif = null;
		/* remove any trailing slash from `path', if not just "/" */
		if ((path.charAt(path.length() - 1) == '/') && (path.length() > 1)) {
			i = MFileSystem.opendir (path.substring (0, path.length() - 1), fh);
		} else {
			i = MFileSystem.opendir (path, fh);
		}
		if (i < 0) {
			fh.refcount = 0;
			current.syscall = null;
			return i;
		} else {
			current.files[fd] = fh;
		}
		
		current.syscall = null;
		return fd;
	}
	//}}}
	//{{{  public static int close (int fd)
	/**
	 * This method closes the given descriptor.
	 *
	 * @param fd file-descriptor
	 *
	 * @return 0 on success, otherwise &lt; 0 error-code
	 */
	public static int close (int fd)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MFile fh;

		if ((fd < 0) || (fd >= current.files.length)) {
			MProcess.sync_process_signals (current);
			return -MSystem.EBADF;
		} else if (current.files[fd] == null) {
			MProcess.sync_process_signals (current);
			return -MSystem.EBADF;
		}

		current.syscall = "close";

		fh = current.files[fd];
		current.files[fd] = null;
		fh.refcount--;
		if (fh.refcount == 0) {
			int error;

			if (fh.fileif == null) {
				/* directory */
				error = fh.dirif.close (fh);
			} else {
				error = fh.fileif.close (fh);
			}

			if (error != 0) {
				/* denied..! -- put things right */
				fh.refcount++;
				current.files[fd] = fh;
				current.syscall = null;
				return error;
			}
		}
		MProcess.sync_process_signals (current);
		current.syscall = null;
		return 0;
	}
	//}}}
	//{{{  public static int read (int fd, byte buffer[], int count)
	/**
	 * this performs a read from the given descriptor.
	 *
	 * @param fd file-descriptor to read from
	 * @param buffer buffer where data will be stored
	 * @param count maximum number of bytes to read
	 *
	 * @return number of bytes read on success, 0 on end-of-file, or -ve on error
	 */
	public static int read (int fd, byte buffer[], int count)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MFile fh;
		int code;

		MProcess.sync_process_signals (current);
		if ((fd < 0) || (fd >= current.files.length)) {
			return -MSystem.EBADF;
		} else if (current.files[fd] == null) {
			return -MSystem.EBADF;
		}

		current.syscall = "read";

		code = current.files[fd].fileif.read (current.files[fd], buffer, count);
		MProcess.sync_process_signals (current);

		current.syscall = null;

		return code;
	}
	//}}}
	//{{{  public static int readdir (int fd, MDirEnt dirent)
	/**
	 * reads a directory entry from an open directory.
	 *
	 * @param fd file-descriptor referring to an open directory
	 * @param dirent structure where the directory information will be placed
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error.
	 */
	public static int readdir (int fd, MDirEnt dirent)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MFile fh;
		int code;

		if ((fd < 0) || (fd >= current.files.length)) {
			return -MSystem.EBADF;
		} else if (current.files[fd] == null) {
			return -MSystem.EBADF;
		}

		fh = current.files[fd];

		if (fh.dirif == null) {
			return -MSystem.EBADF;
		} else if (dirent == null) {
			return -MSystem.EFAULT;
		}

		current.syscall = "readdir";
		code = fh.dirif.readdir (fh, dirent);
		current.syscall = null;

		return code;
	}
	//}}}
	//{{{  public static int write (int fd, byte buffer[], int count)
	/**
	 * this performs a write to the given descriptor.
	 *
	 * @param fd file-descriptor to write to
	 * @param buffer data to write
	 * @param count maximum number of bytes to write
	 *
	 * @return number of bytes written on success, or -ve on error
	 */
	public static int write (int fd, byte buffer[], int count)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MFile fh;
		int code;

		MProcess.sync_process_signals (current);
		if ((fd < 0) || (fd >= current.files.length)) {
			return -MSystem.EBADF;
		} else if (current.files[fd] == null) {
			return -MSystem.EBADF;
		}
		current.syscall = "write";

		code = current.files[fd].fileif.write (current.files[fd], buffer, count);
		MProcess.sync_process_signals (current);

		current.syscall = null;
		return code;
	}
	//}}}
	//{{{  public static int writestring (int fd, String str)
	/**
	 * writes a Java "String" to the given descriptor (more useful than necessary)
	 *
	 * @param fd file-descriptor
	 * @param str string to write
	 *
	 * @return number of bytes written on success, of -ve on error
	 */
	public static int writestring (int fd, String str)
	{
		byte bytes[] = str.getBytes();

		return write (fd, bytes, bytes.length);
	}
	//}}}
	//{{{  public static int fcntl (int fd, int op, int arg)
	/**
	 * file-handle control.  Used to get/set certain options on file-descriptors.
	 *
	 * @param fd file-descriptor of some fcntl capable stream
	 * @param op operation (F_... in MFileOps)
	 * @param arg operation-specific argument
	 *
	 * @return &gt;= 0 on success, or &lt; 0 indicating error
	 */
	public static int fcntl (int fd, int op, int arg)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MFile fh;
		int code;

		MProcess.sync_process_signals (current);
		if ((fd < 0) || (fd >= current.files.length)) {
			return -MSystem.EBADF;
		} else if (current.files[fd] == null) {
			return -MSystem.EBADF;
		}
		fh = current.files[fd];
		current.syscall = "fcntl";

		switch (op) {
		case MFileOps.F_GETFL:		/* get file-descriptor flags */
			code = fh.flags;
			break;
		case MFileOps.F_SETFL:		/* set file-descriptor flags */
			if (fh.fileif == null) {
				code = -MSystem.EIO;
			} else {
				code = fh.fileif.fcntl (fh, op, arg);
			}
			break;
		default:		/* other/unsupported */
			code = -MSystem.EINVAL;
			break;
		}
		MProcess.sync_process_signals (current);

		current.syscall = null;
		return code;
	}
	//}}}
	//{{{  public static int unlink (String path)
	/**
	 * unlinks (removes) a file or directory
	 *
	 * @param path path to the file/directory to remove
	 * 
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int unlink (String path)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int i;

		MProcess.sync_process_signals (current);

		current.syscall = "unlink";
		/* remove any trailing slash from `path', if not just "/" */
		if ((path.length() > 1) && (path.charAt(path.length() - 1) == '/')) {
			i = MFileSystem.unlink (path.substring (0, path.length() - 1));
		} else {
			i = MFileSystem.unlink (path);
		}

		current.syscall = null;
		return i;
	}
	//}}}
	//{{{  public static int opennmq (String name)
	/**
	 * opens a named message-queue.  this returns a file descriptor,
	 * although the application/kernel must use it with readmsg() and writemsg()
	 */
	public static int opennmq (String name)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int fd, i;
		MFile fh;

		MProcess.sync_process_signals (current);

		for (fd = 0; fd < current.files.length; fd++) {
			if (current.files[fd] == null) {
				break;
			}
		}
		if (fd == current.files.length) {
			MProcess.sync_process_signals (current);
			return -MSystem.EMFILE;
		}

		current.syscall = "opennmq";

		fh = new MFile ();
		fh.refcount = 1;
		fh.pdata = null;
		fh.offset = 0;
		fh.fileif = null;
		fh.dirif = null;

		i = MNamedMsgQ.nmq_open (fh, name);
		if (i < 0) {
			/* failed to open */
			fh.refcount = 0;
			current.syscall = null;
			return i;
		} else {
			current.files[fd] = fh;
		}

		current.syscall = null;
		return fd;
	}
	//}}}
	//{{{  public static int writemsg (int fd, int type, Object msg)
	/**
	 * sends a message to a named message-queue
	 *
	 * @param fd file-handle of the named message-queue
	 * @param msg object message
	 * @param type application-defined type
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int writemsg (int fd, int type, Object msg)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int i;

		MProcess.sync_process_signals (current);
		if ((fd < 0) || (fd >= current.files.length)) {
			return -MSystem.EBADF;
		} else if (current.files[fd] == null) {
			return -MSystem.EBADF;
		}

		current.syscall = "writemsg";
		i = MNamedMsgQ.nmq_send (current.files[fd], type, msg);
		current.syscall = null;

		return i;
	}
	//}}}
	//{{{  public static int readmsg (int fd, MNamedMsgQ.MNamedMsg msgret)
	/**
	 * reads a message from a named message-queue
	 *
	 * @param fd file-handle of the named message-queue
	 * @param msgret message return structure
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int readmsg (int fd, MNamedMsgQ.MNamedMsg msgret)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int i;

		MProcess.sync_process_signals (current);
		if ((fd < 0) || (fd >= current.files.length)) {
			return -MSystem.EBADF;
		} else if (current.files[fd] == null) {
			return -MSystem.EBADF;
		}

		current.syscall = "readmsg";
		i = MNamedMsgQ.nmq_recv (current.files[fd], msgret);
		current.syscall = null;

		return i;
	}
	//}}}
	//{{{  public static int socket (int domain, int type)
	/**
	 * this creates a new socket
	 *
	 * @param domain domain of this socket
	 * @param type type of this socket
	 *
	 * @return file-descriptor on success or &lt; 0 indicating error
	 */
	public static int socket (int domain, int type)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int fd, i;
		MFile fh;

		MProcess.sync_process_signals (current);

		for (fd=0; fd<current.files.length; fd++) {
			if (current.files[fd] == null) {
				break;
			}
		}
		if (fd == current.files.length) {
			MProcess.sync_process_signals (current);
			return -MSystem.EMFILE;
		}

		current.syscall = "socket";

		fh = new MFile ();
		fh.refcount = 1;
		fh.pdata = null;
		fh.offset = 0;
		fh.fileif = null;
		fh.dirif = null;

		/* this just creates the socket "endpoint" */
		i = MSocket.make_endpoint (fh, domain, type);
		if (i < 0) {
			/* failed to create socket endpoint */
			fh.refcount = 0;
			current.syscall = null;
			return i;
		} else {
			current.files[fd] = fh;
		}

		current.syscall = null;
		return fd;
	}
	//}}}
	//{{{  public static int bind (int fd, MSocketAddr addr)
	/**
	 * binds a socket to an address
	 *
	 * @param fd file-descriptor for a socket
	 * @param addr address to bind
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int bind (int fd, MSocketAddr addr)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public static int listen (int fd, int backlog)
	/**
	 * enables a socket to listen for incoming connections
	 *
	 * @param fd file-descriptor for a bound socket
	 * @param backlog connection backlog
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int listen (int fd, int backlog)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public static int accept (int fd, MSocketAddr addr)
	/**
	 * accepts an incoming connection from a listening socket
	 *
	 * @param fd file-descriptor for a listening socket
	 * @param addr address in which the connecting client address is stored
	 *
	 * @return a file-descriptor for the connected client on success, or &lt; 0 indicating error
	 */
	public static int accept (int fd, MSocketAddr addr)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public static int connect (int fd, MSocketAddr addr)
	/**
	 * connects a socket to a remote host
	 *
	 * @param fd file-descriptor for a socket
	 * @param addr address of remote host (and port)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int connect (int fd, MSocketAddr addr)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public static void reschedule ()
	/**
	 * this performes a rescheduling operation.  The current process is added to the back
	 * of the run-queue and a reschedule occurs
	 */
	public static void reschedule ()
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];

		MProcess.sync_process_signals (current);
		current.syscall = "[resched]";
		MKernel.add_to_run_queue (current);
		MKernel.schedule ();
		MProcess.sync_process_signals (current);
		current.syscall = null;
	}
	//}}}
	//{{{  public static int forkexecc (String cname, String args[])
	/**
	 * this performs a typical fork()/exec() used to start a new process
	 * 
	 * @param cname Java class name to load (should implement MUserProcess)
	 * @param args arguments to pass to the started process
	 *
	 * @return process-ID on success, or &lt; 0 indicating error
	 */
	public static int forkexecc (String cname, String args[])
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int npid;

		MProcess.sync_process_signals (current);
		/* attempt to start process */
		current.syscall = "forkexecc";
		npid = MProcess.create_user_process (new String(cname), current,
				(String[])args.clone(), MProcess.INHERIT_OPEN_FILES);
		/* in theory, that should be it... */
		current.syscall = null;
		return npid;
	}
	//}}}
	//{{{  public static int forkexec (String cmd, String args[])
	/**
	 * this performs a typical fork()/exec() to start a new process,
	 * but tries to find it via the file-system and MExec
	 *
	 * @param cmd program name to run
	 * @param args arguments to pass to the started process
	 *
	 * @return process-ID on success, or &lt; 0 indicating error
	 */
	public static int forkexec (String cmd, String args[])
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int npid;

		MProcess.sync_process_signals (current);
		/* attempt to start process */
		current.syscall = "forkexec";
		npid = MProcess.create_user_process (new String(cmd), current,
				(String[])args.clone(), MProcess.INHERIT_OPEN_FILES);
		/* in theory, that should be it... */
		current.syscall = null;
		return npid;
	}
	//}}}
	//{{{  public static int writeklog (String str)
	/**
	 * writes a string to the kernel log
	 *
	 * @param str string to write
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int writeklog (String str)
	{
		MKernel.log_msg (str);
		return 0;
	}
	//}}}
	//{{{  public static int loadmodule (String mod, String args[])
	/**
	 * this loads a new kernel module (something that implements MKernelProcess)
	 *
	 * @param mod path to module to run
	 * @param args arguments to pass to the module
	 *
	 * @return process-ID on success, or &lt; 0 indicating error
	 */
	public static int loadmodule (String mod, String args[])
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int npid;

		MProcess.sync_process_signals (current);
		/* attempt to start process */
		current.syscall = "loadmodule";
		npid = MProcess.create_kernel_process (new String(mod), (String[])args.clone());
		/* in theory, that should be it... */
		current.syscall = null;
		return npid;
	}
	//}}}
	//{{{  public static void exit (int exitcode)
	/**
	 * this is called by a process to terminate.  This never returns.
	 *
	 * @param exitcode return code for parent process
	 */
	public static void exit (int exitcode)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];

		current.syscall = "exit";
		/* closes any open files, etc. */
		current.terminate_process (exitcode);

		/* in theory, we never get this far.. */
		synchronized (current) {
			/* deadlock is about the best we can do.. */
			try {
				current.wait ();
			} catch (InterruptedException e) {}
		}
	}
	//}}}
	//{{{  public static int pause ()
	/**
	 * this is called to put a process to sleep (indefinitely)
	 *
	 * @return error-code indicating why the process woke up (EINTR)
	 */
	public static int pause ()
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];

		if (MProcess.sync_process_signals (current)) {
			return -MSystem.EINTR;
		}
		current.syscall = "pause";

		synchronized (current) {
			current.state = MProcess.TASK_SLEEPING;
		}
		MKernel.schedule ();
		MProcess.sync_process_signals (current);
		current.syscall = null;
		return -MSystem.EINTR;
	}
	//}}}
	//{{{  public static int getpid ()
	/**
	 * used to retrieve the current process ID
	 *
	 * @return process ID
	 */
	public static int getpid ()
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];

		return current.pid;
	}
	//}}}
	//{{{  public static int getppid ()
	/**
	 * used to retrieve the parent process ID
	 *
	 * @return process ID or -1 if no parent (init-task or kernel-processes only)
	 */
	public static int getppid ()
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];

		if (current.parent != null) {
			return current.parent.pid;
		}
		return -1;
	}
	//}}}
	//{{{  public static int signal (int sig, int action)
	/**
	 * sets a process's handling of a particular signal
	 *
	 * @param sig signal constant (MSignal.SIG...)
	 * @param action signal action constant (MSignal.SIG_...)
	 *
	 * @return previous signal action for this signal, or &lt; 0 indicating error
	 */
	public static int signal (int sig, int action)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int cact;

		if ((sig <= 0) || (sig > MSignal.SIG_NSIGS)) {
			return -MSystem.EINVAL;
		}

		synchronized (current) {
			cact = current.sig_handling[sig];
			switch (action) {
			case MSignal.SIG_DFL:
			case MSignal.SIG_IGN:
			case MSignal.SIG_CATCH:
				current.sig_handling[sig] = action;
				break;
			default:
				cact = -MSystem.EINVAL;
				break;
			}
		}
		MProcess.sync_process_signals (current);
		return cact;
	}
	//}}}
	//{{{  public static int kill (int pid, int sig)
	/**
	 * sends a signal to a process
	 *
	 * @param pid ID of process to signal
	 * @param sig signal number (MSignal.SIG...)
	 *
	 * @return zero on success, otherwise -ve indicating error
	 */
	public static int kill (int pid, int sig)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MProcess other;
		MSignal signal;
		
		if ((pid < 0) || (sig < 0) || (sig >= MSignal.SIG_NSIGS))  {
			return -MSystem.EINVAL;
		}
		other = MKernel.find_process (pid);

		if (other == null) {
			return -MSystem.ESRCH;
		}

		current.syscall = "kill";

		signal = new MSignal (sig, null);
		MKernel.queue_signal (other, signal);

		MProcess.sync_process_signals (current);

		current.syscall = null;
		return 0;
	}
	//}}}
	//{{{  public static int[] wait (boolean nohang)
	/**
	 * this method waits for a child process to exit (nohang is false), or
	 * polls for an exited child (nohang is true).
	 *
	 * This is done by waiting for SIGCHLD signals to be delivered, rather than
	 * anything more elaborate.  Any other signals will be delivered before returning.
	 *
	 * @param nohang if true, this method will not sleep if no child processes have exited
	 *
	 * @return an array of integers.  These come in pairs, i.e. [pid0, exitcode0, pid1, exitcode1, ...].
	 * 		If no child processes have exited, or the process interrupted, null is returned
	 */
	public static int[] wait (boolean nohang)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MWaitQueue sleeping;
		int ra[] = null;
		int nexited = 0;

		current.syscall = "wait";
		while (true) {
			synchronized (current) {
				//{{{  see if any children have exited yet
				if (current.signalled) {
					for (MSignal sq = current.pending_signals; sq != null; sq = sq.next) {
						if (sq.signo == MSignal.SIGCHLD) {
							/* got one */
							nexited++;
						}
					}
				}
				//}}}
				if (nexited > 0) {
					//{{{  yes, extract SIGCHLDs, deliver others and return
					MSignal prev = null;
					MSignal next = null;

					ra = new int[nexited * 2];
					nexited = 0;
					for (MSignal sq = current.pending_signals; sq != null; sq = next) {
						next = sq.next;
						if (sq.signo == MSignal.SIGCHLD) {
							/* extract info */
							int data[] = (int[])(sq.sigdata);

							ra[nexited++] = data[0];
							ra[nexited++] = data[1];

							/* remove this one */
							if (prev == null) {
								/* first */
								current.pending_signals = next;
								sq = null;		/* ensure prev is sensible on the next iteration */
							} else {
								/* not the first */
								prev.next = next;
								sq = prev;		/* ensure prev stays the same next time */
							}
						}
						prev = sq;
					}
					if (current.pending_signals == null) {
						current.signalled = false;
					}
					if (current.signalled) {
						/* done whilst we have the lock on "current" */
						MKernel.deliver_process_signals (current);
					}
					current.syscall = null;
					return ra;
					//}}}
				} else {
					//{{{  deliver any other signals and return if we did (i.e. interrupted)
					if (current.signalled) {
						MKernel.deliver_process_signals (current);
						current.syscall = null;
						return null;
					}
					//}}}
				}
				/* if we shouldn't wait around, get out now */
				if (nohang) {
					current.syscall = null;
					return null;
				}
				/* sleep.. */
				sleeping = new MWaitQueue ();
				sleeping.add_to_queue (current);
				current.state = MProcess.TASK_SLEEPING;
			}
			/* if we get here, we're going to sleep.. */
			MKernel.schedule ();

			/* must have been signalled (or other good reason for wakeup) if we get here */
			/* get off the wait queue */
			sleeping.del_from_queue (current);

			/* then we loop -- in theory, a signal should have been delivered, in which case
			 * it will be processed accordingly, and return.  If not, we set nohang in any case,
			 * so null would be returned.
			 */
			nohang = true;
		}

	}
	//}}}
	//{{{  public static MProcess[] process_list ()
	/**
	 * used to get the current process list.  The application must
	 * format the resulting data.
	 *
	 * @return array of process information blocks
	 */
	public static MProcess[] process_list ()
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MProcess p_info[];

		current.syscall = "process_list";
		p_info = MKernel.get_process_list ();
		MProcess.sync_process_signals (current);
		current.syscall = null;
		return p_info;
	}
	//}}}
	//{{{  public static int sleep (long millis)
	/**
	 * this is used to put a process to sleep for a given amount of time
	 *
	 * @param millis timeout in milli-seconds
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int sleep (long millis)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		MWaitQueue sleeping;

		sleeping = new MWaitQueue ();
		current.syscall = "sleep";
		synchronized (current) {
			sleeping.add_to_queue (current);
			current.state = MProcess.TASK_SLEEPING;
			MTimer.add_to_timer_queue (current, millis);
		}
		MKernel.schedule ();
		/* woke up, remove from queues */
		sleeping.del_from_queue (current);
		MTimer.del_from_timer_queue (current);
		if (MProcess.sync_process_signals (current)) {
			current.syscall = null;
			return -MSystem.EINTR;
		}
		current.syscall = null;
		return 0;
	}
	//}}}
	//{{{  public static int sendmsg (int pid, int type, Object message)
	/**
	 * this is used to send a message to another process
	 *
	 * @param pid target process PID
	 * @param type message type (application specific)
	 * @param message object message (application specific)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int sendmsg (int pid, int type, Object message)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int code;

		if ((pid < 0) || (type < 0) || (message == null)) {
			return -MSystem.EINVAL;
		}
		/* we won't send a message to a process that doesn't exist yet */
		if (MKernel.find_process (pid) == null) {
			return -MSystem.ESRCH;
		}
		current.syscall = "sendmsg";
		code = MMailBox.sendmsg (current.pid, pid, type, message);
		current.syscall = null;
		return code;
	}
	//}}}
	//{{{  public static Object recvmsg (int pid, int type)
	/**
	 * this is used to receive a message from another (or any) process
	 *
	 * @param pid specific PID to receive from (or -1 for any)
	 * @param type specific message type to receive (application specific, or -1 for any)
	 *
	 * @return the object message, or null on error (bad pid/type or interrupted by a signal)
	 */
	public static Object recvmsg (int pid, int type)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		Object result;

		if ((pid < -1) || (type < -1)) {
			return null;
		}
		current.syscall = "recvmsg";
		result = MMailBox.recvmsg (pid, current.pid, type);
		current.syscall = null;
		return result;
	}
	//}}}
	//{{{  public static int semop (int op, int key, int value)
	/**
	 * this is used to perform a semaphore operation.
	 *
	 * @param op semaphore operation (SEMOP_CREATE, SEMOP_SET)
	 * @param key semaphore key (identifier)
	 * @param value initial value when creating, or adjustment when setting
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int semop (int op, int key, int value)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int code;

		if (key < 0) {
			return -MSystem.EINVAL;
		}
		MProcess.sync_process_signals (current);
		current.syscall = "semop";
		switch (op) {
		case MSemaphore.SEMOP_CREATE:
			code = MSemaphore.semcreate (key, value);
			break;
		case MSemaphore.SEMOP_SET:
			code = MSemaphore.semset (current, key, value);
			break;
		case MSemaphore.SEMOP_REMOVE:
			code = MSemaphore.semremove (key);
			break;
		default:
			code = -MSystem.EINVAL;
			break;
		}
		current.syscall = null;
		return code;
	}
	//}}}
	//{{{  public static int mount (String path, String fstype, String options[])
	/**
	 * this is used to mount a file-system.
	 *
	 * @param path absolute path to mount point
	 * @param fstype file-system type to mount
	 * @param options file-system specific options (may include device)
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int mount (String path, String fstype, String options[])
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int code;

		if ((path == null) || (path.length() == 0) || (path.charAt(0) != '/')) {
			return -MSystem.EINVAL;
		}
		if ((fstype == null) || (fstype.length() == 0)) {
			return -MSystem.EINVAL;
		}

		current.syscall = "mount";
		
		/* remove any trailing slash from `path', as long as it's not just "/" */
		if ((path.charAt(path.length() - 1) == '/') && (path.length() > 1)) {
			code = MFileSystem.mount (path.substring (0, path.length() - 1), fstype, options);
		} else {
			code = MFileSystem.mount (path, fstype, options);
		}
		current.syscall = null;
		return code;
	}
	//}}}
	//{{{  public static int umount (String path)
	/**
	 * this is used to unmount a file-system.
	 *
	 * @param path absolute path of the file-system to unmount (mount-point)
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int umount (String path)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int code;

		if ((path == null) || (path.length() == 0) || (path.charAt(0) != '/')) {
			return -MSystem.EINVAL;
		}
		current.syscall = "umount";

		/* remove any trailing slash from `path', as long as it's not just "/" */
		if ((path.charAt(path.length() - 1) == '/') && (path.length() > 1)) {
			code = MFileSystem.umount (path.substring (0, path.length() - 1));
		} else {
			code = MFileSystem.umount (path);
		}

		current.syscall = null;
		return code;
	}
	//}}}
	//{{{  public static int mkdir (String path, int mode)
	/**
	 * creates a directory
	 *
	 * @param path absolute path to the directory to be created
	 * @param mode permissions to be used when creating the directory
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int mkdir (String path, int mode)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int code;

		if (path == null) {
			return -MSystem.EFAULT;
		}
		current.syscall = "mkdir";
		/* remove any trailing slash from `path' */
		if (path.charAt(path.length() - 1) == '/') {
			code = MFileSystem.mkdir (path.substring (0, path.length() - 1), mode);
		} else {
			code = MFileSystem.mkdir (path, mode);
		}
		current.syscall = null;
		return code;
	}
	//}}}
	//{{{  public static int stat (String path, MInode statbuf)
	/**
	 * stats a file or directory
	 *
	 * @param path absolute path to the file/directory to be `stat'd
	 * @param statbuf MInode where the information will be placed
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int stat (String path, MInode statbuf)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int code;

		if ((path == null) || (statbuf == null)) {
			return -MSystem.EFAULT;
		}
		current.syscall = "stat";
		/* remove any trailing slash from `path' */
		if (path.charAt(path.length() - 1) == '/') {
			code = MFileSystem.stat (path.substring (0, path.length() - 1), statbuf);
		} else {
			code = MFileSystem.stat (path, statbuf);
		}
		current.syscall = null;
		return code;
	}
	//}}}
	//{{{  public static int umask (int mask)
	/**
	 * sets a process's umask.  This is the file-creation mask, e.g. if umask=022
	 * and a mode is specified as 666, the actual mode will be: (666 | ~022) = 644 (rw-r--r--)
	 *
	 * @param mask the new umask
	 *
	 * @return the previous umask
	 */
	public static int umask (int mask)
	{
		MProcess current = MKernel.current[MProcessor.currentCPU()];
		int cmask = current.umask;

		current.umask = mask & 0777;
		return cmask;
	}
	//}}}
	//{{{  public static int access (String path, int amode)
	/**
	 * test to see if a file can be opened in the given mode.  Modes are
	 * F_OK, R_OK, W_OK and X_OK.
	 *
	 * @param path path to file/directory to check
	 * @param amode access modes to check for
	 *
	 * @return 0 on success (all modes granted), or &lt; 0 indicating error
	 */
	public static int access (String path, int amode)
	{
		amode &= (MFileOps.R_OK | MFileOps.F_OK | MFileOps.W_OK | MFileOps.X_OK);
		if (amode == 0) {
			return -MSystem.EINVAL;
		}

		return MFileSystem.access (path, amode);
	}
	//}}}
	//{{{  public static int setpriority (int pri)
	/**
	 * sets the priority of the calling process
	 *
	 * @param pri new process priority
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int setpriority (int pri)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	
	public static Boolean setPriority(int pid, ProcessPriorityEnum priority) {
		return MKernel.setProcessPriority(pid, priority);
	}
}


