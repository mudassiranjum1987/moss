/*
 *	MHostFS.java -- host file-system
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

package moss.fs;

import moss.kernel.*;
import moss.user.*;

import java.util.*;
import java.io.*;


/**
 * this class implements the "host" file-system.  This mirrors the host file-system;
 * the option specifies which root to mount if there are many.
 */

public class MHostFS implements MFSOps
{
	//{{{  private variables/clases/methods/etc.
	
	//{{{  private interface MHostFSEnt
	/**
	 * this class defines an "entity" in the object file-system
	 */
	private interface MHostFSEnt
	{
		/** gets the name of this entity */
		public String get_name ();
		/** gets the inode of this entity */
		public MInode get_inode ();

		/** returns true if this entity is a directory */
		public boolean isdir ();

	}
	//}}}
	//{{{  private class MHostFSDir implements MDirOps, MHostFSEnt
	
	private class MHostFSDir implements MDirOps, MHostFSEnt
	{
		/** file reference of this directory */
		public File path;
		/** inode for this directory */
		public MInode inode;
		/** open reference count */
		public int refcount;

		/** contents of this directory */
		public MDirEnt contents[];

		//{{{  private synchronized void rebuildcontents ()
		/**
		 * rebuilds the contents of the directory
		 */
		private synchronized void rebuildcontents ()
		{
			String lpath = path.toString ();
			int llen = lpath.length ();
			File list[];
			int i;

			if ((llen > 1) && (lpath.charAt (llen - 1) != '/')) {
				llen++;
			}
			list = path.listFiles ();
			contents = new MDirEnt[list.length];

			for (i=0; i<list.length; i++) {
				contents[i] = new MDirEnt ();
				contents[i].d_ino = 0;
				contents[i].d_name = list[i].toString().substring (llen);
				if (list[i].isDirectory()) {
					contents[i].d_mode = MInode.S_IFDIR | 0111;
				} else {
					contents[i].d_mode = MInode.S_IFREG;
				}
				if (list[i].canRead()) {
					contents[i].d_mode |= 0444;
				}
				if (list[i].canWrite()) {
					contents[i].d_mode |= 0200;
				}
			}
			return;
		}
		//}}}

		//{{{  public MHostFSDir (File path, MInode inode)
		public MHostFSDir (File path, MInode inode)
		{
			this.path = path;
			this.inode = inode;
			this.refcount = 0;
			this.contents = null;
		}
		//}}}

		//{{{  public synchronized int open (MFile handle)
		public synchronized int open (MFile handle)
		{
			handle.pdata = (Object)this;
			handle.dirif = this;
			handle.offset = 0;

			rebuildcontents ();
			refcount++;
			return 0;
		}
		//}}}
		//{{{  public synchronized int close (MFile handle)
		public synchronized int close (MFile handle)
		{
			handle.pdata = null;

			refcount--;
			if (refcount == 0) {
				synchronized (openstuff) {
					openstuff.remove ((Object)path.toString());
				}
			}
			return 0;
		}
		//}}}
		//{{{  public synchronized int readdir (MFile handle, MDirEnt dirent)
		public synchronized int readdir (MFile handle, MDirEnt dirent)
		{
			int dirlen;
			
			dirlen = contents.length;
			if (handle.offset >= dirlen) {
				return -MSystem.ESPIPE;		/* "illegal seek" is probably about the best we can do.. */
			}
			dirent.d_name = new String (contents[handle.offset].d_name);
			dirent.d_ino = contents[handle.offset].d_ino;
			dirent.d_mode = contents[handle.offset].d_mode;
			handle.offset++;

			return 0;
		}
		//}}}

		//{{{  public String get_name ()
		public String get_name ()
		{
			return path.toString();
		}
		//}}}
		//{{{  public MInode get_inode ()
		public MInode get_inode ()
		{
			return inode;
		}
		//}}}
		//{{{  public boolean isdir ()
		/**
		 * returns true.
		 */
		public boolean isdir ()
		{
			return true;
		}
		//}}}

	}

	//}}}
	//{{{  private class MHostFSFile implements MFileOps, MHostFSEnt
	
	private class MHostFSFile implements MFileOps, MHostFSEnt
	{
		/** path of this file */
		public File path;
		/** inode for this file */
		public MInode inode;
		/** reference count */
		public int refcount;
		/** file-access object */
		public RandomAccessFile randfile;

		/** contents of this file */
		public byte data[];

		//{{{  public MHostFSFile (File path, MInode inode)
		public MHostFSFile (File path, MInode inode)
		{
			this.path = path;
			this.inode = inode;
			this.data = null;
			this.refcount = 0;

			if (path.exists()) {
				inode.size = path.length ();
			}
		}
		//}}}

		//{{{  public synchronized int open (MFile handle, int flags)
		public synchronized int open (MFile handle, int flags)
		{
			handle.pdata = (Object)this;
			handle.fileif = this;
			handle.offset = 0;

			if (refcount == 0) {
				String modestr;

				/* first to open this file */
				modestr = ((flags & MFileOps.OPEN_WRITE) != 0) ? "rw" : "r";
				try {
					randfile = new RandomAccessFile (path, modestr);
				} catch (Exception e) {
					randfile = null;
				}
				if (randfile == null) {
					synchronized (openstuff) {
						openstuff.remove ((Object)path.toString());
					}
					return -MSystem.EIO;		/* non-specific */
				}
			}
			refcount++;
			return 0;
		}
		//}}}
		//{{{  public synchronized int close (MFile handle)
		public synchronized int close (MFile handle)
		{
			handle.pdata = null;

			refcount--;
			if (refcount == 0) {
				synchronized (openstuff) {
					openstuff.remove ((Object)path.toString());
				}
			}

			return 0;
		}
		//}}}
		//{{{  public synchronized int lseek (MFile handle, int offset, int whence)
		public synchronized int lseek (MFile handle, int offset, int whence)
		{
			/* make offset absolute */
			switch (whence) {
			case LSEEK_BEG:
				break;
			case LSEEK_CUR:
				offset = handle.offset + offset;
				break;
			case LSEEK_END:
				offset = (int)inode.size - offset;
				break;
			}
			if (offset < 0) {
				offset = 0;
			} else if (offset > inode.size) {
				offset = (int)inode.size;
			}

			handle.offset = offset;

			return handle.offset;
		}
		//}}}
		//{{{  public synchronized int read (MFile handle, byte buffer[], int count)
		public synchronized int read (MFile handle, byte buffer[], int count)
		{
			int r;

			if (handle.offset >= inode.size) {
				/* end-of-file */
				return 0;
			}
			if (count > (inode.size - handle.offset)) {
				count = ((int)inode.size - handle.offset);
			}

			/* read some data */
			try {
				randfile.seek (handle.offset);
				r = randfile.read (buffer, 0, count);
				if (r < 0) {
					r = 0;		/* end-of-file */
				}
				handle.offset += r;
			} catch (IOException ioe) {
				r = -MSystem.EIO;
			}

			return r;
		}
		//}}}
		//{{{  public synchronized int write (MFile handle, byte buffer[], int count)
		public synchronized int write (MFile handle, byte buffer[], int count)
		{
			int r;

			/* write some data */
			try {
				byte tmpbuf[] = new byte[count];
				long moffs;

				System.arraycopy (buffer, 0, tmpbuf, 0, count);
				randfile.seek (handle.offset);
				randfile.write (buffer, 0, count);
				moffs = randfile.getFilePointer ();
				if (moffs != (handle.offset + count)) {
					/* didn't write it all! */
					r = (int)moffs - handle.offset;
					handle.offset = (int)moffs;
				} else {
					handle.offset = (int)moffs;
					r = count;
				}
			} catch (IOException ioe) {
				r = -MSystem.EIO;
			}

			/* might have enlarged the file here */
			if (handle.offset > inode.size) {
				inode.size = handle.offset;		/* got bigger */
			}

			return r;
		}
		//}}}
		//{{{  public synchronized int fcntl (MFile handle, int op, int arg)
		public synchronized int fcntl (MFile handle, int op, int arg)
		{
			/* not yet.. */
			return -MSystem.EIO;
		}
		//}}}

		//{{{  public boolean isdir ()
		/**
		 * returns false.
		 */
		public boolean isdir ()
		{
			return false;
		}
		//}}}
		//{{{  public synchronized String get_name ()
		public synchronized String get_name ()
		{
			return path.toString ();
		}
		//}}}
		//{{{  public synchronized MInode get_inode ()
		public synchronized MInode get_inode ()
		{
			return inode;
		}
		//}}}
	}

	//}}}

	/** real roots */
	private File realroots[];

	/** open files and directories */
	private Hashtable openstuff;

	/** ever-incrementing inode counter (!) */
	private int icount;

	//{{{  private MHostFSEnt newfsent (File path, int mode, int uid, int gid)
	private MHostFSEnt newfsent (File path, int mode, int uid, int gid)
	{
		MHostFSEnt ent;
		MInode ino = new MInode ();

		if ((mode & MInode.S_IFMT) == MInode.S_IFDIR) {
			ent = new MHostFSDir (path, ino);
		} else {
			ent = new MHostFSFile (path, ino);
		}
		ino.ino = icount++;
		ino.mode = mode;
		ino.nlinks = 1;
		ino.uid = uid;
		ino.gid = gid;

		return ent;
	}
	//}}}
	//{{{  private MHostFSEnt newfsent (File path, MHostFSEnt entity)
	private MHostFSEnt newfsent (File path, MHostFSEnt entity)
	{
		MHostFSEnt ent;
		MInode ino = entity.get_inode ();

		if ((ino.mode & MInode.S_IFMT) == MInode.S_IFDIR) {
			/* duplicating directory link */
			ent = new MHostFSDir (path, ino);
		} else {
			/* duplicating ordinary entry */
			ent = new MHostFSFile (path, ino);
		}
		ino.nlinks++;

		return ent;
	}
	//}}}

	//}}}


	//{{{  private MHostFSEnt lookup (String path)
	/**
	 * looks up an existing MHostFSEnt based on the path.
	 *
	 * @param path path to file/directory
	 * 
	 * @return MHostFSEnt object or null
	 */
	private MHostFSEnt lookup (String path)
	{
		Object r;
		
		synchronized (openstuff) {
			r = openstuff.get (path);
		}

		if (r != null) {
			return (MHostFSEnt)r;
		}
		return null;
	}
	//}}}


	//{{{  public int mount (String mount_path, String options[])
	/**
	 * called when the object file-system is mounted.  Performs specific initialisation.
	 *
         * @param mount_path full path from the root (/) to where this is being mounted.  A single
	 * 			"/" indicates that this is being mounted as the root file-system.
	 * @param options file-system specific options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mount (String mount_path, String options[])
	{
		int i;

		/* initialise */
		realroots = File.listRoots ();

		if (realroots.length > 1) {
			MKernel.log_msg ("host file-system does not support multiple roots, yet..");
			return -MSystem.EIO;
		} else if (realroots.length < 1) {
			MKernel.log_msg ("no roots!");
			return -MSystem.EIO;
		}

		icount = 1;
		openstuff = new Hashtable ();

		// MKernel.log_msg ("MHostFS: host file-system roots:");
		// for (i=0; i<realroots.length; i++) {
		// 	MKernel.log_msg ("    " + realroots[i].toString());
		// }

		// MKernel.log_msg ("MHostFS: mounting on [" + mount_path + "]");

		return 0;
	}
	//}}}
	//{{{  public int umount (String options[])
	/**
	 * this is called before the object file-system is un-mounted.
	 *
	 * @param options file-system specific un-mounting options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int umount (String options[])
	{
		if (openstuff.size() > 0) {
			return -MSystem.EBUSY;
		}

		/* clean-up */
		openstuff = null;
		realroots = null;
		icount = 0;

		return 0;
	}
	//}}}
	//{{{  public int open (String path, MFile handle, int flags, int mode)
	/**
	 * called to open a file.
	 *
	 * @param path relative path to the file (e.g. `etc/passwd' for the root file-system)
	 * @param handle file-handle to be associated with this file
	 * @param flags open flags
	 * @param mode file mode if creating
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int open (String path, MFile handle, int flags, int mode)
	{
		File fpath;
		MHostFSEnt ent;

		// MKernel.log_msg ("MHostFS::open(): path=[" + path + "]");

		fpath = new File (realroots[0].toString() + path);
		if (!fpath.exists() && ((flags & MFileOps.OPEN_CREAT) == 0)) {
			return -MSystem.ENOENT;
		} else if (fpath.exists() && fpath.isDirectory()) {
			return -MSystem.EISDIR;
		}
		ent = lookup (path);

		if (ent == null) {
			ent = newfsent (fpath, mode, 0, 0);

			synchronized (openstuff) {
				openstuff.put ((Object)path, (Object)ent);
			}
		}

		return ((MHostFSFile)ent).open (handle, flags);
	}
	//}}}
	//{{{  public int opendir (String path, MFile handle)
	/**
	 * called to open a directory (read-only!).
	 *
	 * @param path relative path to the directory (e.g. `etc/init.d' for the root file-system)
	 * @param handle file-handle to be associated with this directory
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int opendir (String path, MFile handle)
	{
		File fpath;
		MHostFSEnt ent;

		// MKernel.log_msg ("MHostFS::opendir(): path=[" + path + "]");

		fpath = new File (realroots[0].toString() + path);

		if (!fpath.exists ()) {
			return -MSystem.ENOENT;
		} else if (!fpath.isDirectory()) {
			return -MSystem.ENOTDIR;
		}
		ent = lookup (path);
		if (ent == null) {
			int mode = MInode.S_IFDIR;

			if (fpath.canRead()) {
				mode |= 0555;
			}
			if (fpath.canWrite()) {
				mode |= 0200;
			}
			ent = newfsent (fpath, mode, 0, 0);

			synchronized (openstuff) {
				openstuff.put ((Object)path, (Object)ent);
			}
		}

		return ((MHostFSDir)ent).open (handle);
	}
	//}}}
	//{{{  public int mkdir (String path)
	/**
	 * creates a directory in the file-system
	 *
	 * @param path relative path to the directory to be created
	 * @param flags flags (mode)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mkdir (String path, int flags)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int unlink (String path)
	/**
	 * removes a name from the file-system
	 *
	 * @param path relative path to the name to be removed
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int unlink (String path)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int rmdir (String path)
	/**
	 * removes a directory from the file-system (must be empty)
	 *
	 * @param path relative path to the directory to be removed
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int rmdir (String path)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int link (String oldpath, String newpath)
	/**
	 * creates a `hard-link'
	 *
	 * @param oldpath existing relative path (e.g. local/packages/nmh for "usr" file-system)
	 * @param newpath new relative path
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int link (String oldpath, String newpath)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int symlink (String oldpath, String newpath)
	/**
	 * creates a symbolic link
	 *
	 * @param oldpath full path to existing name (e.g. /usr/local/packages/nmh-1.4 for "usr" file-system)
	 * @param newpath relative path to new link (e.g. local/packages/nmh for "usr" file-system)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int symlink (String oldpath, String newpath)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int mknod (String path, int mode, int majmin)
	/**
	 * creates a special file on the file-system
	 *
	 * @param path relative path to file to create
	 * @param mode file mode flags
	 * @param majmin major and minor numbers `<tt>((major &lt;&lt; 16) | minor)</tt>' for block/char special files,
	 * 			otherwise ignored.
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mknod (String path, int mode, int majmin)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int readlink (String path, byte[] buf, int buflen)
	/**
	 * reads the contents of a symbolic-link on the file-system
	 *
	 * @param path relative path to symbolic link on file-system
	 * @param buf buffer where the link data is stored
	 * @param buflen maximum number of bytes to write into `buf'
	 *
	 * @return number of bytes read on success, or &lt; 0 indicating error
	 */
	public int readlink (String path, byte[] buf, int buflen)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int utime (String path, long[] times)
	/**
	 * sets access and modification times for a file (inode)
	 *
	 * @param path relative path to name
	 * @param times array of two `<tt>long</tt>'s that are the access and modification times respectively
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int utime (String path, long[] times)
	{
		return -MSystem.ENOSYS;
	}
	//}}}
	//{{{  public int stat (String path, MInode statbuf)
	/**
	 * retrieves information about a name on the file-system
	 *
	 * @param path relative path to name
	 * @param statbuf buffer in which information is placed
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int stat (String path, MInode statbuf)
	{
		File fpath;
		MHostFSEnt ent;

		fpath = new File (realroots[0].toString() + path);

		if (!fpath.exists ()) {
			return -MSystem.ENOENT;
		}
		ent = lookup (path);
		if (ent != null) {
			synchronized (ent) {
				MInode ent_inode = ent.get_inode();

				statbuf.major = ent_inode.major;
				statbuf.minor = ent_inode.minor;
				statbuf.ino = ent_inode.ino;
				statbuf.mode = ent_inode.mode;
				statbuf.nlinks = ent_inode.nlinks;
				statbuf.uid = ent_inode.uid;
				statbuf.gid = ent_inode.gid;
				statbuf.size = ent_inode.size;
				statbuf.blksize = ent_inode.blksize;
				statbuf.nblocks = ent_inode.nblocks;
				statbuf.atime = ent_inode.atime;
				statbuf.mtime = ent_inode.mtime;
				statbuf.ctime = ent_inode.ctime;
			}
		} else {
			statbuf.major = 0;
			statbuf.minor = 0;
			statbuf.ino = 0;
			statbuf.nlinks = 1;
			statbuf.uid = 0;
			statbuf.gid = 0;
			statbuf.size = fpath.length ();
			statbuf.blksize = 512;
			statbuf.nblocks = (statbuf.size / statbuf.blksize);
			statbuf.atime = 0;
			statbuf.mtime = 0;
			statbuf.ctime = 0;
			if (fpath.isDirectory ()) {
				statbuf.mode = 0111;
				statbuf.mode |= MInode.S_IFDIR;
			} else {
				statbuf.mode = 0;
			}
			if (fpath.canRead()) {
				statbuf.mode |= 0444;
			}
			if (fpath.canWrite()) {
				statbuf.mode |= 0200;
			}
		}

		return 0;
	}
	//}}}
	//{{{  public int access (String path, int amode)
	/**
	 * tests for access to a file
	 *
	 * @param path relative path to file/directory
	 * @param amode access mode
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int access (String path, int amode)
	{
		File fpath;

		fpath = new File (realroots[0].toString() + path);
		if (!fpath.exists()) {
			return -MSystem.ENOENT;
		}
		
		if (((amode & MFileOps.R_OK) != 0) && !fpath.canRead()) {
			return -MSystem.EPERM;
		}
		if (((amode & MFileOps.W_OK) != 0) && !fpath.canWrite()) {
			return -MSystem.EPERM;
		}
		/* execute -- take as readable for now.. */
		if (((amode & MFileOps.X_OK) != 0) && !fpath.canRead()) {
			return -MSystem.EPERM;
		}

		return 0;
	}
	//}}}

}


