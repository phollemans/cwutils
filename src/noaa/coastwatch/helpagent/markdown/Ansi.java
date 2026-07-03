/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent.markdown;

/**
 * ANSI terminal escape-code constants for text styling.
 *
 * @author Peter Hollemans
 * @since 4.2.0
 */
public final class Ansi {

  /** The escape introducer. */
  public static final String ESC = "\u001B[";

  /** Resets all attributes. */
  public static final String RESET = ESC + "0m";

  /** Enables bold text. */
  public static final String BOLD = ESC + "1m";

  /** Enables faint text. */
  public static final String FAINT = ESC + "2m";

  /** Enables italic text. */
  public static final String ITALIC = ESC + "3m";

  /** Enables underlined text. */
  public static final String UNDERLINE = ESC + "4m";

  /** Reverses foreground and background colors. */
  public static final String REVERSE = ESC + "7m";

  /** Sets the default foreground color. */
  public static final String FG_DEFAULT = ESC + "39m";

  /** Sets black foreground. */
  public static final String FG_BLACK = ESC + "30m";

  /** Sets red foreground. */
  public static final String FG_RED = ESC + "31m";

  /** Sets green foreground. */
  public static final String FG_GREEN = ESC + "32m";

  /** Sets yellow foreground. */
  public static final String FG_YELLOW = ESC + "33m";

  /** Sets blue foreground. */
  public static final String FG_BLUE = ESC + "34m";

  /** Sets magenta foreground. */
  public static final String FG_MAGENTA = ESC + "35m";

  /** Sets cyan foreground. */
  public static final String FG_CYAN = ESC + "36m";

  /** Sets white foreground. */
  public static final String FG_WHITE = ESC + "37m";

  /** Sets bright black foreground. */
  public static final String FG_BRIGHT_BLACK = ESC + "90m";

  /** Sets bright red foreground. */
  public static final String FG_BRIGHT_RED = ESC + "91m";

  /** Sets bright green foreground. */
  public static final String FG_BRIGHT_GREEN = ESC + "92m";

  /** Sets bright yellow foreground. */
  public static final String FG_BRIGHT_YELLOW = ESC + "93m";

  /** Sets bright blue foreground. */
  public static final String FG_BRIGHT_BLUE = ESC + "94m";

  /** Sets bright magenta foreground. */
  public static final String FG_BRIGHT_MAGENTA = ESC + "95m";

  /** Sets bright cyan foreground. */
  public static final String FG_BRIGHT_CYAN = ESC + "96m";

  /** Sets bright white foreground. */
  public static final String FG_BRIGHT_WHITE = ESC + "97m";

  /** The regex to match ANSI escape codes. */
  public static final String ANSI_CODE_REGEX = "\u001B\\[[0-9;]*[A-Za-z]";

  ////////////////////////////////////////////////////////////

  /** Prevents construction. */
  private Ansi () { }

  ////////////////////////////////////////////////////////////

} // Ansi
