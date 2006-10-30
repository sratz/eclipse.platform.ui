/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jface.databinding.observable.tree;

import org.eclipse.jface.databinding.observable.IDiff;

/**
 * @since 1.1
 * 
 */
public abstract class TreeDiff implements IDiff {

	/**
	 * Returns the tree path (possibly empty) of the parent, or
	 * <code>null</code> if this tree diff is a child of another tree diff
	 * (only the root tree diff must return a non-null path).
	 * 
	 * @return the tree path (possibly empty) of the unchanged parent
	 */
	public abstract TreePath getParentPath();

	/**
	 * 
	 */
	public final static int NO_CHANGE = 0x00;

	/**
	 * 
	 */
	public final static int ADDED = 0x01;

	/**
	 * 
	 */
	public final static int REMOVED = 0x02;

	/**
	 * 
	 */
	public final static int REPLACED = 0x03;

	/**
	 * @return
	 */
	public abstract int getChangeType();

	/**
	 * @return the element that was removed, or the replaced element
	 */
	public abstract Object getOldElement();

	/**
	 * @return the element that was not changed, added, or the replacement
	 *         element
	 */
	public abstract Object getNewElement();

	/**
	 * @return the index at which the element was added, removed, or replaced
	 */
	public abstract int getIndex();

	/**
	 * Returns the child tree diff objects that describe changes to children. If
	 * the change type is REMOVED, there will be no children.
	 * 
	 * @return
	 */
	public abstract TreeDiff[] getChildren();

	/**
	 * Accepts the given visitor.
	 * 
	 * @param visitor
	 */
	public void accept(TreeDiffVisitor visitor) {
		doAccept(visitor, getParentPath());
	}

	private void doAccept(TreeDiffVisitor visitor, TreePath parentPath) {
		TreePath currentPath = parentPath.createChildPath(getNewElement());
		boolean recurse = visitor.visit(this, currentPath);
		if (recurse) {
			TreeDiff[] children = getChildren();
			for (int i = 0; i < children.length; i++) {
				TreeDiff child = children[i];
				child.doAccept(visitor, currentPath);
			}
		}
	}

}
