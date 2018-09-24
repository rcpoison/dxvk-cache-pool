/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ignorelist.kassandra.dxvk.cache.pool.common.model;

import com.google.common.base.Equivalence;
import java.util.Objects;

/**
 *
 * @author poison
 */
public class ExecutableInfoEquivalenceFileName extends Equivalence<ExecutableInfo> {

	@Override
	protected boolean doEquivalent(ExecutableInfo a, ExecutableInfo b) {
		return Objects.equals(a.getFileName(), b.getFileName());
	}

	@Override
	protected int doHash(ExecutableInfo t) {
		return Objects.hashCode(t.getFileName());
	}

}
