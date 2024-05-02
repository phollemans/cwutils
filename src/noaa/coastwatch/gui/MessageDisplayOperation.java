/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.util.function.Consumer;

import java.util.logging.Logger;

/**
 * 
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

  public void showMessage (String message) { if (showAction != null) showAction.accept (message); }

  ////////////////////////////////////////////////////////////

  public void dispose() { if (disposeAction != null) disposeAction.run(); }

  ////////////////////////////////////////////////////////////

  public void cancel() { if (cancelAction != null) cancelAction.run(); }

  ////////////////////////////////////////////////////////////

  public void setCancelAction (Runnable action) { this.cancelAction = action; }

  ////////////////////////////////////////////////////////////

  public void setShowAction (Consumer<String> action) { this.showAction = action; }

  ////////////////////////////////////////////////////////////

  public void setDisposeAction (Runnable action) { this.disposeAction = action; }

  ////////////////////////////////////////////////////////////

} // MessageDisplayOperation class

