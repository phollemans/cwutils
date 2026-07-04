/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.util.function.Consumer;

import java.util.logging.Logger;

/**
 * The <code>MessageDisplayOperation</code> class stores actions used to
 * show, cancel, and dispose of a displayed message.
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class MessageDisplayOperation {

  private static final Logger LOGGER = Logger.getLogger (MessageDisplayOperation.class.getName());    

  private Runnable cancelAction;
  private Consumer<String> showAction;
  private Runnable disposeAction;

  ////////////////////////////////////////////////////////////

  /**
   * Shows a message.
   *
   * @param message the message text.
   */
  public void showMessage (String message) { if (showAction != null) showAction.accept (message); }

  ////////////////////////////////////////////////////////////

  /** Disposes of the current message. */
  public void dispose() { if (disposeAction != null) disposeAction.run(); }

  ////////////////////////////////////////////////////////////

  /** Cancels the current message. */
  public void cancel() { if (cancelAction != null) cancelAction.run(); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the cancel action.
   *
   * @param action the cancel action.
   */
  public void setCancelAction (Runnable action) { this.cancelAction = action; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the show action.
   *
   * @param action the show action.
   */
  public void setShowAction (Consumer<String> action) { this.showAction = action; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the dispose action.
   *
   * @param action the dispose action.
   */
  public void setDisposeAction (Runnable action) { this.disposeAction = action; }

  ////////////////////////////////////////////////////////////

} // MessageDisplayOperation class
