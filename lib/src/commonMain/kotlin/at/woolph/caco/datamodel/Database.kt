/* Copyright 2025 Wolfgang Mayer */
package at.woolph.caco.datamodel

import at.woolph.caco.HomeDirectory

expect fun initDatabase(homeDirectory: HomeDirectory = HomeDirectory())
