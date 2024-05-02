/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2024 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.gui;

import java.util.Map;
import java.util.HashMap;

/**
 *
 * 
 * 
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class DispatchTable implements RequestHandler {

  private String typeID;
  private Map<String, Runnable> commandToRunnableMap;

  public DispatchTable (String typeID) { 

    this.typeID = typeID; 
    this.commandToRunnableMap = new HashMap<>(); 

  } // DispatchTable

  public void addDispatch (String command, Runnable runnable) { commandToRunnableMap.put (command, runnable); }

  @Override
  public void handleRequest (Request request) { 

    var command = (String) request.getContent();
    var runnable = commandToRunnableMap.get (command);
    if (runnable != null) runnable.run();
    else throw new IllegalArgumentException ("No dispatch runnable for request " + command);

  } // handleRequest
  
  @Override
  public boolean canHandleRequest (Request request) { return (request.getTypeID().equals (this.typeID)); }

} // DispatchTable class