package com.alexscode.utilities.dao;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Flags a constructor to be used by VirtualTable
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DaoConstructor {
}
