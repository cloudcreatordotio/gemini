/*******************************************************************************
 * Copyright (c) 2018, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.techempower.security;

import java.security.*;
import java.util.*;

/**
 * Provides random tokens for validation.  Like
 * {@link com.techempower.gemini.form.FormNonce}, this class can be used to
 * protect against cross-site request forgery, though this class may be used
 * outside of forms (in AJAX requests, for example).
 * <p>
 * Tokens generated by the provider will be considered valid until they are
 * consumed by calls to {@link #validate(byte[])}.  The provider
 * has a maximum capacity specified in the constructor, and if a call to
 * {@link #next()} causes the provider to become full, the oldest
 * token will be removed.
 * 
 * <pre>
 * TokenProvider provider = new TokenProvider(1, 8); //stores an 8-byte token
 * byte[] foo = provider.next();
 * byte[] bar = provider.next();
 * provider.validate(foo); // Returns false - foo was pushed out by bar.
 * provider.validate(bar); // Returns true.
 * provider.validate(bar); // Returns false, bar was consumed above.
 * </pre>
 */
public class TokenProvider
{
  private int capacity;
  private int tokenSize;
  private LinkedList<byte[]> tokens;
  private Random random;
  
  /**
   * Creates a token provider.  
   * 
   * @param capacity The maximum number of valid tokens that can exist at a
   *                 given time.
   * @param tokenSize The size in bytes of each token.
   * @throws IllegalArgumentException if {@code capacity} or {@code tokenSize} 
   *                                  are less than one.
   */
  public TokenProvider(int capacity, int tokenSize)
  {
    if (capacity < 1)
    {
      throw new IllegalArgumentException(
          "The capacity must be greater than zero.");
    }
    
    if (tokenSize < 1)
    {
      throw new IllegalArgumentException(
          "The token size must be greater than zero.");
    }
    
    this.capacity = capacity;
    this.tokenSize = tokenSize;
    this.tokens = new LinkedList<>();
    
    try
    {
      this.random = SecureRandom.getInstance("SHA1PRNG");
    }
    catch (NoSuchAlgorithmException e)
    {
      this.random = new SecureRandom();
    }
  }
  
  /**
   * Returns a new valid token.  If the provider has reached maximum capacity, 
   * the oldest token will be removed.
   * 
   * @return A new token.
   */
  public synchronized byte[] next()
  {
    if (this.tokens.size() >= this.capacity)
    {
      this.tokens.removeLast();
    }
    byte[] token = new byte[this.tokenSize];
    this.random.nextBytes(token);
    this.tokens.addFirst(token);
    return token;
  }
  
  /**
   * Checks whether {@code token} is valid, consuming it in the process.
   * 
   * @param token A token.
   * @return {@code true} If the token is valid.
   */
  public synchronized boolean validate(byte[] token)
  {
    for (Iterator<byte[]> iter = this.tokens.iterator(); iter.hasNext();)
    {
      if (MessageDigest.isEqual(token, iter.next()))
      {
        iter.remove();
        return true;
      }
    }
    
    return false;
  }
}