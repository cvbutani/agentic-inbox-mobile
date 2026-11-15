package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.runtime.Composable

/**
 * True when the user has asked the system for less motion: Android's "Remove animations" /
 * animator duration scale of 0, iOS's Reduce Motion. Anything that moves continuously (the
 * [AirmailBand]) must hold still and signal its state another way when this is set.
 */
@Composable
expect fun rememberReduceMotion(): Boolean
