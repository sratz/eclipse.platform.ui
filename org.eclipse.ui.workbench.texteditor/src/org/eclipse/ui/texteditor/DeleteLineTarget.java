package org.eclipse.ui.texteditor;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * A delete line target.
 * @since 2.1
 */
class DeleteLineTarget {

	/**
	 * A clipboard which concatenates subsequent delete line actions.
	 */
	private class DeleteLineClipboard implements MouseListener, ModifyListener, ISelectionChangedListener, ITextListener, FocusListener {

		/** The text viewer. */
		private final ITextViewer fViewer;
		/*
		 * This is a hack to stop a string of deletions when the user moves
		 * the caret. This kludge is necessary since:
		 * 1) Moving the caret does not fire a selection event
		 * 2) There is no support in StyledText for a CaretListener
		 * 3) The AcceleratorScope and KeybindingService classes are internal
		 * 
		 * This kludge works by comparing the offset of the caret to the offset
		 * recorded the last time the action was run. If they differ, we do not
		 * continue the session.
		 * 
		 * @see #saveState
		 * @see #checkState
		 */
		/** The last known offset of the caret */
		private int fIndex= -1;
		/** The clip board. */
		private Clipboard fClipboard;
		/** A string buffer. */
		private final StringBuffer fBuffer= new StringBuffer();
		/** The deleting flag. */
		private boolean fDeleting;

		/**
		 * Creates the text viewer.
		 */
		public DeleteLineClipboard(ITextViewer viewer) {
			Assert.isNotNull(viewer);
			fViewer= viewer;	
		}
		
		public ITextViewer getViewer() {
			return fViewer;	
		}
		
		/**
		 * Saves the current state, to be compared later using
		 * <code>checkState</code>.
		 */
		private void saveState() {
			fIndex= fViewer.getTextWidget().getCaretOffset();
		}
	
		/**
		 * Checks that the state has not changed since it was saved.
		 * 
		 * @return returns <code>true</code> if the current state is the same as
		 * when it was last saved.
		 */
		private boolean hasSameState() {
			return fIndex == fViewer.getTextWidget().getCaretOffset();
		}
		
		public void checkState() {

			if (fClipboard == null) {
				StyledText text= fViewer.getTextWidget();
				if (text == null)
					return;

				fViewer.getSelectionProvider().addSelectionChangedListener(this);
				text.addFocusListener(this);
				text.addMouseListener(this);
				text.addModifyListener(this);

				fClipboard= new Clipboard(text.getDisplay());
				fBuffer.setLength(0);

			} else if (!hasSameState()) {
				fBuffer.setLength(0);
			}
		}

		public void append(String deltaString) {
			fBuffer.append(deltaString);
			String string= fBuffer.toString();
			Transfer[] dataTypes= new Transfer[] { TextTransfer.getInstance() };
			Object[] data= new Object[] { string };
			fClipboard.setContents(data, dataTypes);
		}
		
		/**
		 * Uninstalls this action.
		 */
		private void uninstall() {
	
			if (fClipboard == null)
				return;
	
			StyledText text= fViewer.getTextWidget();
			if (text == null)
				return;
	
			fViewer.getSelectionProvider().removeSelectionChangedListener(this);
			text.removeFocusListener(this);
			text.removeMouseListener(this);
			text.removeModifyListener(this);
	
			fClipboard.dispose();
			fClipboard= null;
		}

		public void setDeleting(boolean deleting) {
			fDeleting= deleting;	
		}
	
		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(MouseEvent)
		 */
		public void mouseDoubleClick(MouseEvent e) {
			uninstall();
		}
	
		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseDown(MouseEvent)
		 */
		public void mouseDown(MouseEvent e) {
			uninstall();
		}
	
		/*
		 * @see org.eclipse.swt.events.MouseListener#mouseUp(MouseEvent)
		 */
		public void mouseUp(MouseEvent e) {
			uninstall();
		}
	
		/*
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			uninstall();
		}
	
		/*
		 * @see org.eclipse.swt.events.FocusListener#focusGained(FocusEvent)
		 */
		public void focusGained(FocusEvent e) {
			uninstall();
		}
	
		/*
		 * @see org.eclipse.swt.events.FocusListener#focusLost(FocusEvent)
		 */
		public void focusLost(FocusEvent e) {
			uninstall();
		}
	
		/*
		 * @see org.eclipse.jface.text.ITextListener#textChanged(TextEvent)
		 */
		public void textChanged(TextEvent event) {
			uninstall();
		}
	
		/*
		 * @see org.eclipse.swt.events.ModifyListener#modifyText(ModifyEvent)
		 */
		public void modifyText(ModifyEvent e) {
			if (!fDeleting)
				uninstall();
		}		
	}

	/** The clipboard manager. */
	private final DeleteLineClipboard fClipboard;

	/**
	 * Constructor for DeleteLineTarget.
	 */
	public DeleteLineTarget(ITextViewer viewer) {
		fClipboard= new DeleteLineClipboard(viewer);
	}

	private static IRegion getDeleteRegion(IDocument document, int position, int type) throws BadLocationException {

		int line= document.getLineOfOffset(position);
		int offset= 0;
		int length= 0;

		switch  (type) {
		case DeleteLineAction.WHOLE:
			offset= document.getLineOffset(line);
			length= document.getLineLength(line);
			break;

		case DeleteLineAction.TO_BEGINNING:
			offset= document.getLineOffset(line);
			length= position - offset;
			break;

		case DeleteLineAction.TO_END:		
			offset= position;

			IRegion lineRegion= document.getLineInformation(line);
			int end= lineRegion.getOffset() + lineRegion.getLength();

			if (position == end) {
				String lineDelimiter= document.getLineDelimiter(line);
				length= lineDelimiter == null ? 0 : lineDelimiter.length();

			} else {
				length= end - offset;
			}
			break;
						
		default:
			throw new IllegalArgumentException();
		}
		
		return new Region(offset, length);
	}
	
	/**
	 * Deletes the specified fraction of the line of the given offset.
	 * 
	 * @param document the document
	 * @param position the offset
	 * @param type the specification of what to delete
	 * @throws BadLocationException if position is not valid in the given document
	 */
	public void deleteLine(IDocument document, int position, int type) throws BadLocationException {

		IRegion deleteRegion= getDeleteRegion(document, position, type);
		int offset= deleteRegion.getOffset();
		int length= deleteRegion.getLength();
		
		if (length == 0)
			return;

		fClipboard.checkState();
		fClipboard.append(document.get(offset, length));

		fClipboard.setDeleting(true);
		document.replace(offset, length, null);
		fClipboard.setDeleting(false);

		fClipboard.saveState();
	}

}