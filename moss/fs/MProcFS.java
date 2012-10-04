/*
 *	MProcFS.java -- process file-system
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

/**
 * this class implements the "proc" file-system.  This is one way in which user-processes
 * can inspect various bits of the kernel (e.g. loaded modules, mounted file-systems, etc.)
 */

public class MProcFS implements MFSOps
{
	//{{{  private variables/clases/methods/etc.
	
	//{{{  private interface MProcFSEnt
	/**
	 * this class defines an "entity" in the proc file-system
	 */
	private interface MProcFSEnt
	{
		/** gets the name of this entity */
		public String get_name ();
		/** gets the inode of this entity */
		public MInode get_inode ();

		/** returns true if this entity is a directory */
		public boolean isdir ();

	}
	//}}}
	//{{{  private static class MProcFSDir implements MDirOps
	
	private static class MProcFSDir implements MDirOps, MProcFSEnt
	{
		/** name of this directory */
		public String name;
		/** inode for this directory */
		public MInode inode;

		/** contents of this directory */
		public ArrayList contents;
		public Hashtable dirhash;

		//{{{  public MProcFSDir (String name, MInode inode)
		public MProcFSDir (String name, MInode inode)
		{
			this.name = name;
			this.inode = inode;
			this.contents = new ArrayList ();
			this.dirhash = new Hashtable ();
		}
		//}}}

		//{{{  public synchronized int open (MFile handle)
		public synchronized int open (MFile handle)
		{
			handle.pdata = (Object)this;
			handle.dirif = this;
			handle.offset = 0;

			return 0;
		}
		//}}}
		//{{{  public synchronized int close (MFile handle)
		public synchronized int close (MFile handle)
		{
			handle.pdata = null;

			return 0;
		}
		//}}}
		//{{{  public synchronized int readdir (MFile handle, MDirEnt dirent)
		public synchronized int readdir (MFile handle, MDirEnt dirent)
		{
			int dirlen = contents.size ();
			MProcFSEnt ent;

			if (handle.offset >= dirlen) {
				return -MSystem.ESPIPE;		/* "illegal seek" is probably about the best we can do.. */
			}
			ent = (MProcFSEnt) contents.get (handle.offset);
			dirent.d_name = new String(ent.get_name ());
			dirent.d_ino = (ent.get_inode ()).ino;
			dirent.d_mode = (ent.get_inode ()).mode;
			handle.offset++;

			return 0;
		}
		//}}}

		//{{{  public String get_name ()
		public String get_name ()
		{
			return name;
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

		//{{{  public synchronized void addtodir (MProcFSEnt ent)
		public synchronized void addtodir (MProcFSEnt ent)
		{
			String entname = ent.get_name ();
			Object item = dirhash.get (entname);

			if (item != null) {
				int diridx = contents.lastIndexOf (item);
				MProcFSEnt dent = (MProcFSEnt)item;

				if (diridx < 0) {
					MKernel.panic ("MProcFS: entity in directory hash but not in contents.");
				}
				dent.get_inode().nlinks--;
				dirhash.remove (entname);
				contents.remove (diridx);
			}
			/* add it */
			contents.add ((MProcFSEnt)ent);
			dirhash.put (entname, (MProcFSEnt)ent);
			
			return;
		}
		//}}}
		//{{{  public synchronized void delfromdir (MProcFSEnt ent)
		public synchronized void delfromdir (MProcFSEnt ent)
		{
			String entname = ent.get_name ();
			Object item = dirhash.get (entname);
			int didx;

			if (item == null) {
				/* doesn't exist! */
				return;
			}

			didx = contents.lastIndexOf (item);

			dirhash.remove (entname);
			contents.remove (didx);

			return;
		}
		//}}}
		//{{{  public MProcFSEnt lookup (String name)
		public synchronized MProcFSEnt lookup (String name)
		{
			Object item = dirhash.get (name);

			if (item != null) {
				return (MProcFSEnt)item;
			}
			return null;
		}
		//}}}
	}

	//}}}
	//{{{  private static class MProcFSFile implements MFileOps, MProcFSEnt
	
	private static class MProcFSFile implements MFileOps, MProcFSEnt
	{
		/** name of this file */
		public String name;
		/** inode for this file */
		public MInode inode;
		/** implementation for this file */
		public MProcFSIf impl;


		/** this private class is linked into individual files and holds the buffer there */
		private class PFSHandle
		{
			public byte data[];
		}

		//{{{  public MProcFSFile (String name, MInode inode, MProcFSIf impl)
		public MProcFSFile (String name, MInode inode, MProcFSIf impl)
		{
			this.name = name;
			this.inode = inode;
			this.impl = impl;
		}
		//}}}

		//{{{  public synchronized int open (MFile handle, int flags)
		public synchronized int open (MFile handle, int flags)
		{
			PFSHandle priv = new PFSHandle ();

			priv.data = null;
			handle.pdata = (Object)priv;
			handle.fileif = this;
			handle.offset = 0;

			return 0;
		}
		//}}}
		//{{{  public synchronized int close (MFile handle)
		public synchronized int close (MFile handle)
		{
			handle.pdata = null;

			return 0;
		}
		//}}}
		//{{{  public synchronized int lseek (MFile handle, int offset, int whence)
		public synchronized int lseek (MFile handle, int offset, int whence)
		{
			PFSHandle priv = (PFSHandle)handle.pdata;

			if (priv.data == null) {
				/* do the actual proc read here */
				String data;

				data = impl.readproc (inode, name);
				if (data == null) {
					return -MSystem.EIO;
				}
				priv.data = data.getBytes ();
			}

			/* make offset absolute */
			switch (whence) {
			case LSEEK_BEG:
				break;
			case LSEEK_CUR:
				offset = handle.offset + offset;
				break;
			case LSEEK_END:
				offset = (int)priv.data.length - offset;
				break;
			}
			if (offset < 0) {
				offset = 0;
			} else if (offset > priv.data.length) {
				offset = (int)priv.data.length;
			}

			handle.offset = offset;

			return handle.offset;
		}
		//}}}
		//{{{  public synchronized int read (MFile handle, byte buffer[], int count)
		public synchronized int read (MFile handle, byte buffer[], int count)
		{
			PFSHandle priv = (PFSHandle)handle.pdata;

			if (priv.data == null) {
				/* do the actual proc read here */
				String data = impl.readproc (inode, name);

				if (data == null) {
					return -MSystem.EIO;
				}
				priv.data = data.getBytes ();
			}

			if (handle.offset >= priv.data.length) {
				/* end-of-file */
				return 0;
			}

			if (count > (priv.data.length - handle.offset)) {
				count = (priv.data.length - handle.offset);
			}

			/* read some data */
			System.arraycopy (priv.data, handle.offset, buffer, 0, count);
			handle.offset += count;

			return count;
		}
		//}}}
		//{{{  public synchronized int write (MFile handle, byte buffer[], int count)
		public synchronized int write (MFile handle, byte buffer[], int count)
		{
			/* not yet.. */
			return -MSystem.EIO;
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
			return name;
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

	//{{{  private static class PFS_procfs implements MProcFSIf
	/**
	 * this class deals with the contents of /proc/procfs
	 */
	private static class PFS_procfs implements MProcFSIf
	{
		public String readproc (MInode inode, String name)
		{
			String r;

			r = "MProcFS file-system\n";

			return r;
		}
	}
	//}}}

	/** general lock for static data */
	private static Object synclock = new Object ();
	/** root of the file-system (only ever 1 of these) */
	private static MProcFSEnt root = null;
	/** ever-incrementing inode counter (!) */
	private static int icount = 0;
	/** mount count */
	private static int mount_count = 0;

	//{{{  private static MProcFSEnt newfsent (String name, int mode, int uid, int gid, MProcFSIf impl)
	private static MProcFSEnt newfsent (String name, int mode, int uid, int gid, MProcFSIf impl)
	{
		MProcFSEnt ent;
		MInode ino = new MInode ();

		if ((mode & MInode.S_IFMT) == MInode.S_IFDIR) {
			ent = new MProcFSDir (new String (name), ino);
		} else {
			ent = new MProcFSFile (new String (name), ino, impl);
		}
		ino.ino = icount++;
		ino.mode = mode;
		ino.nlinks = 1;
		ino.uid = uid;
		ino.gid = gid;

		return ent;
	}
	//}}}
	//{{{  private static MProcFSEnt newfsent (String name, MProcFSEnt entity)
	private static MProcFSEnt newfsent (String name, MProcFSEnt entity)
	{
		MProcFSEnt ent;
		MInode ino = entity.get_inode ();

		if ((ino.mode & MInode.S_IFMT) == MInode.S_IFDIR) {
			/* duplicating directory link */
			ent = new MProcFSDir (new String (name), ino);
		} else {
			/* duplicating ordinary entry */
			ent = new MProcFSFile (new String (name), ino, ((MProcFSFile)entity).impl);
		}
		ino.nlinks++;

		return ent;
	}
	//}}}

	//{{{  private static String[] split_path (String path)
	private static String[] split_path (String path)
	{
		int nbits, j;
		String bits[];
		int pl;

		pl = path.length ();
		if (path.charAt (pl-1) == '/') {
			/* if it ends in a /, lose it */
			pl--;
		}

		nbits = 1;
		for (int i=0; i<pl; i++) {
			if (path.charAt(i) == '/') {
				nbits++;
			}
		}
		bits = new String[nbits];

		j = 0;
		nbits = 0;
		for (int i=0; i<pl; i++) {
			if (path.charAt(i) == '/') {
				bits[nbits] = path.substring (j, i);
				j = i+1;
				nbits++;
			}
		}
		if (j < pl) {
			bits[nbits] = path.substring (j, pl);
		}

		return bits;
	}
	//}}}
	//{{{  private static MProcFSEnt lookup (String bits[])
	/**
	 * starting at the root, this scans the file-system for something
	 */
	private static MProcFSEnt lookup (String bits[])
	{
		MProcFSEnt ent = root;

		for (int idx = 0; idx < bits.length; idx++) {
			MProcFSEnt newent;

			if (!ent.isdir ()) {
				return null;
			}
			newent = ((MProcFSDir)ent).lookup (bits[idx]);
			if (newent == null) {
				return null;
			}
			ent = newent;
		}
		return ent;
	}
	//}}}


	//{{{  private static void init_proc_root ()
	/**
	 * initialises the proc root file-system
	 */
	private static void init_proc_root ()
	{
		MProcFSEnt ent;
		MProcFSDir rootdir;

		synchronized (synclock) {
			if (root != null) {
				return;
			}

			icount = 1;
			root = newfsent ("", (0755 | MInode.S_IFDIR), 0, 0, null);
		}
		rootdir = (MProcFSDir)root;

		ent = newfsent (".", root);
		rootdir.addtodir (ent);
		ent = newfsent ("..", root);
		rootdir.addtodir (ent);

		return;
	}
	//}}}

	//}}}

	//{{{  public static void finalinit ()
	/**
	 * called to perform final kernel initialisation (in the context of the init-task)
	 */
	public static void finalinit ()
	{
		MProcFS.register_procimpl ("procfs", new PFS_procfs());
	}
	//}}}

	//{{{  public static MInode register_procimpl (String name, MProcFSIf impl)
	/**
	 * registers something with the process file-system
	 *
	 * @param name entry-name.  may be something like "foo/bar", but the "foo" must exist first
	 * @param impl implementation
	 *
	 * @return an MInode for the entry, or null on error (if it already exists)
	 */
	public static MInode register_procimpl (String name, MProcFSIf impl)
	{
		MProcFSEnt parent, ent;
		String bits[] = split_path (name);

		init_proc_root ();		/* if not already */

		ent = lookup (bits);
		if (ent != null) {
			/* already exists */
			return null;
		}

		/* creating it */
		if (bits.length == 1) {
			parent = root;
		} else {
			String newbits[] = new String[bits.length - 1];

			System.arraycopy (bits, 0, newbits, 0, newbits.length);
			parent = lookup (newbits);

			if (parent == null) {
				return null;
			}
		}
		ent = newfsent (bits[bits.length - 1], MInode.S_IFREG | 0444, 0, 0, impl);

		/* stick in parent */
		((MProcFSDir)parent).addtodir (ent);

		return ent.get_inode();
	}
	//}}}
	//{{{  public static int unregister_procimpl (String name)
	/**
	 * unregisters something from the process file-system
	 *
	 * @param name entry-name.  may be something like "foo/bar"
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int unregister_procimpl (String name)
	{
		MProcFSEnt ent, parent;
		String bits[] = split_path (name);

		init_proc_root ();		/* make sure.. */

		ent = lookup (bits);
		if (ent == null) {
			return -MSystem.ENOENT;
		}

		if (bits.length == 1) {
			parent = root;
		} else {
			String newbits[] = new String[bits.length - 1];

			System.arraycopy (bits, 0, newbits, 0, newbits.length);
			parent = lookup (newbits);

			if (parent == null) {
				return -MSystem.ENOENT;
			}
		}

		/* remove it */
		((MProcFSDir)parent).delfromdir (ent);
		
		return 0;
	}
	//}}}
	//{{{  public static int register_procdir (String name)
	/**
	 * creates a directory in the proess file-system
	 *
	 * @param name name of the directory to create
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int register_procdir (String name)
	{
		MProcFSEnt ent, parent;
		String bits[] = split_path (name);

		init_proc_root ();		/* make sure.. */

		ent = lookup (bits);
		if (ent != null) {
			return -MSystem.EEXISTS;
		}

		if (bits.length == 1) {
			parent = root;
		} else {
			String newbits[] = new String[bits.length - 1];

			System.arraycopy (bits, 0, newbits, 0, newbits.length);
			parent = lookup (newbits);

			if (parent == null) {
				return -MSystem.ENOENT;
			}
		}
		ent = newfsent (bits[bits.length - 1], (0755 | MInode.S_IFDIR), 0, 0, null);

		/* add it to the parent */
		((MProcFSDir)parent).addtodir (ent);

		/* add the two special directory entries to it */
		((MProcFSDir)ent).addtodir (newfsent (".", ent));
		((MProcFSDir)ent).addtodir (newfsent ("..", parent));

		return 0;
	}
	//}}}
	//{{{  public static int unregister_procdir (String name)
	/**
	 * removes a directory in the process file-system
	 *
	 * @param name name of the directory to remove
	 *
	 * @return 0 on success, otherwise &lt; 0 indicating error
	 */
	public static int unregister_procdir (String name)
	{
		MProcFSEnt ent, parent;
		MProcFSEnt dent, ddent;
		String bits[] = split_path (name);

		init_proc_root ();		/* make sure.. */

		ent = lookup (bits);
		if (ent == null) {
			return -MSystem.ENOENT;
		} else if (!ent.isdir()) {
			return -MSystem.ENOTDIR;
		}

		if (bits.length == 1) {
			parent = root;
		} else {
			String newbits[] = new String[bits.length - 1];

			System.arraycopy (bits, 0, newbits, 0, newbits.length);
			parent = lookup (newbits);

			if (parent == null) {
				return -MSystem.ENOENT;
			}
		}

		/* remove the two special entries */
		{
			String newbits[] = new String[bits.length + 1];
			
			System.arraycopy (bits, 0, newbits, 0, bits.length);
			newbits[bits.length] = ".";
			dent = lookup (newbits);
			newbits[bits.length] = "..";
			ddent = lookup (newbits);
		}

		if ((dent == null) || (ddent == null)) {
			return -MSystem.EIO;
		}

		((MProcFSDir)ent).delfromdir (dent);
		((MProcFSDir)ent).delfromdir (ddent);

		/* remove it */
		((MProcFSDir)parent).delfromdir (ent);
		
		return 0;
	}
	//}}}


	//{{{  public int mount (String mount_path, String options[])
	/**
	 * called when the proc file-system is mounted.  Performs specific initialisation.
	 *
         * @param mount_path full path from the root (/) to where this is being mounted.  A single
	 * 			"/" indicates that this is being mounted as the root file-system.
	 * @param options file-system specific options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mount (String mount_path, String options[])
	{

		// MKernel.log_msg ("MProcFS: mounting on [" + mount_path + "]");

		init_proc_root ();
		synchronized (synclock) {
			mount_count++;
		}
		return 0;
	}
	//}}}
	//{{{  public int umount (String options[])
	/**
	 * this is called before the proc file-system is un-mounted.
	 *
	 * @param options file-system specific un-mounting options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int umount (String options[])
	{
		synchronized (synclock) {
			mount_count--;
		}
		return 0;
	}
	//}}}
	//{{{  public int open (String path, MFile handle, int flags)
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
		MProcFSEnt ent = null;
		MProcFSFile fileent = null;
		String bpath[];

		if (path.equals ("/")) {
			ent = root;
			bpath = new String[] {path};
		} else {
			bpath = split_path (path);
			ent = lookup (bpath);
		}

		if (ent == null) {
			return -MSystem.ENOENT;
		} else if (ent.isdir()) {
			return -MSystem.EISDIR;
		} /* else ent is a valid file */

		fileent = (MProcFSFile)ent;

		if ((flags & MFileOps.OPEN_WRITE) != 0) {
			/* not allowed to write proc-fs stuff (yet..) */
			return -MSystem.EPERM;
		}

		return fileent.open (handle, flags);
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
		MProcFSEnt ent = null;
		MProcFSDir dirent = null;

		if (path.equals ("/")) {
			ent = root;
		} else {
			String bpath[] = split_path (path);

			ent = lookup (bpath);
		}
		if (ent == null) {
			return -MSystem.ENOENT;
		} else if (!ent.isdir()) {
			return -MSystem.ENOTDIR;
		}
		/* permission checks, etc. here */
		dirent = (MProcFSDir)ent;

		/* open it */
		return dirent.open (handle);
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
		/* not the proc file-system.. */
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		return -MSystem.EIO;
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
		MProcFSEnt ent;
		String bits[];

		// MKernel.log_msg ("MProcFS::stat.  path = [" + path + "]");
		if (path.equals ("/")) {
			ent = root;
		} else {
			bits = split_path (path);
			ent = lookup (bits);
		}

		if (ent == null) {
			return -MSystem.ENOENT;
		}

		/* read out inode data */
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
		MProcFSEnt ent;
		String bits[];
		int r = 0;
		int imode;

		if (path.equals ("/")) {
			ent = root;
		} else {
			bits = split_path (path);
			ent = lookup (bits);
		}

		if (ent == null) {
			return -MSystem.ENOENT;
		}
		synchronized (ent) {
			MInode ino = ent.get_inode ();

			imode = ino.mode;
		}
		
		/* these checks are slightly blind.. (check for any read/write/execute) */
		if (((amode & MFileOps.R_OK) != 0) && ((imode & 0444) == 0)) {
			return -MSystem.EPERM;
		}
		if (((amode & MFileOps.W_OK) != 0) && ((imode & 0222) == 0)) {
			return -MSystem.EPERM;
		}
		if (((amode & MFileOps.X_OK) != 0) && ((imode & 0111) == 0)) {
			return -MSystem.EPERM;
		}

		return 0;
	}
	//}}}
}


