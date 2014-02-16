/*******************************************************************************
 * Copyright (c) 2008 Ralf Ebert
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ralf Ebert - initial API and implementation
 *******************************************************************************/
package com.swtxml.util.lang;

@SuppressWarnings("serial")
public class RuntimeIOException extends RuntimeException {

  public RuntimeIOException() {

  }

  public RuntimeIOException(final String message) {
    super(message);
  }

  public RuntimeIOException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public RuntimeIOException(final Throwable cause) {
    super(cause);
  }

}
