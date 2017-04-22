/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.github.core;

/**
 * Programming languages
 */
public enum Language {

	/** ACTIONSCRIPT */
	ACTIONSCRIPT("ActionScript"), //$NON-NLS-1$
	/** ADA */
	ADA("Ada"), //$NON-NLS-1$
	/** APPLESCRIPT */
	APPLESCRIPT("AppleScript"), //$NON-NLS-1$
	/** ARC */
	ARC("Arc"), //$NON-NLS-1$
	/** ASP */
	ASP("ASP"), //$NON-NLS-1$
	/** ASSEMBLY */
	ASSEMBLY("Assembly"), //$NON-NLS-1$
	/** BATCHFILE */
	BATCHFILE("Batchfile"), //$NON-NLS-1$
	/** BEFUNGE */
	BEFUNGE("Befunge"), //$NON-NLS-1$
	/** BLITZMAX */
	BLITZMAX("BlitzMax"), //$NON-NLS-1$
	/** BOO */
	BOO("Boo"), //$NON-NLS-1$
	/** BRAINFUCK */
	BRAINFUCK("Brainfuck"), //$NON-NLS-1$
	/** C */
	C("C"), //$NON-NLS-1$
	/** CSHARP */
	CSHARP("C#"), //$NON-NLS-1$
	/** CPLUSPLUS */
	CPLUSPLUS("C++"), //$NON-NLS-1$
	/** C_OBJDUMP */
	C_OBJDUMP("C-ObjDump"), //$NON-NLS-1$
	/** CHUCK */
	CHUCK("Chuck"), //$NON-NLS-1$
	/** CLOJURE */
	CLOJURE("Clojure"), //$NON-NLS-1$
	/** COFFEESCRIPT */
	COFFEESCRIPT("CoffeeScript"), //$NON-NLS-1$
	/** COLDFUSION */
	COLDFUSION("ColdFusion"), //$NON-NLS-1$
	/** COMMON_LISP */
	COMMON_LISP("Common Lisp"), //$NON-NLS-1$
	/** CPP_OBJDUMP */
	CPP_OBJDUMP("Cpp-ObjDump"), //$NON-NLS-1$
	/** CSS */
	CSS("CSS"), //$NON-NLS-1$
	/** CUCUMBER */
	CUCUMBER("Cucumber"), //$NON-NLS-1$
	/** CYTHON */
	CYTHON("Cython"), //$NON-NLS-1$
	/** D */
	D("D"), //$NON-NLS-1$
	/** D_OBJDUMP */
	D_OBJDUMP("D-ObjDump"), //$NON-NLS-1$
	/** DARCS_PATCH */
	DARCS_PATCH("Darcs Patch"), //$NON-NLS-1$
	/** DELPHI */
	DELPHI("Delphi"), //$NON-NLS-1$
	/** DIFF */
	DIFF("Diff"), //$NON-NLS-1$
	/** DYLAN */
	DYLAN("Dylan"), //$NON-NLS-1$
	/** EIFFEL */
	EIFFEL("Eiffel"), //$NON-NLS-1$
	/** EMACS_LISP */
	EMACS_LISP("Emacs Lisp"), //$NON-NLS-1$
	/** ERLANG */
	ERLANG("Erlang"), //$NON-NLS-1$
	/** FSHARP */
	FSHARP("F#"), //$NON-NLS-1$
	/** FACTOR */
	FACTOR("Factor"), //$NON-NLS-1$
	/** FANCY */
	FANCY("Fancy"), //$NON-NLS-1$
	/** FORTRAN */
	FORTRAN("FORTRAN"), //$NON-NLS-1$
	/** GAS */
	GAS("GAS"), //$NON-NLS-1$
	/** GENSHI */
	GENSHI("Genshi"), //$NON-NLS-1$
	/** GENTOO_EBUILD */
	GENTOO_EBUILD("Gentoo Ebuild"), //$NON-NLS-1$
	/** GENTOO_ECLASS */
	GENTOO_ECLASS("Gentoo Eclass"), //$NON-NLS-1$
	/** GO */
	GO("Go"), //$NON-NLS-1$
	/** GROFF */
	GROFF("Groff"), //$NON-NLS-1$
	/** GROOVY */
	GROOVY("Groovy"), //$NON-NLS-1$
	/** HAML */
	HAML("Haml"), //$NON-NLS-1$
	/** HASKELL */
	HASKELL("Haskell"), //$NON-NLS-1$
	/** HAXE */
	HAXE("HaXe"), //$NON-NLS-1$
	/** HTML */
	HTML("HTML"), //$NON-NLS-1$
	/** HTML_DJANGO */
	HTML_DJANGO("HTML+Django"), //$NON-NLS-1$
	/** HTML_ERB */
	HTML_ERB("HTML+ERB"), //$NON-NLS-1$
	/** HTML_PHP */
	HTML_PHP("HTML+PHP"), //$NON-NLS-1$
	/** INI */
	INI("INI"), //$NON-NLS-1$
	/** IO */
	IO("Io"), //$NON-NLS-1$
	/** IRC_LOG */
	IRC_LOG("IRC log"), //$NON-NLS-1$
	/** JAVA */
	JAVA("Java"), //$NON-NLS-1$
	/** JAVA_SERVER_PAGE */
	JAVA_SERVER_PAGE("Java Server Pages"), //$NON-NLS-1$
	/** JAVASCRIPT */
	JAVASCRIPT("JavaScript"), //$NON-NLS-1$
	/** LILYPOND */
	LILYPOND("LilyPond"), //$NON-NLS-1$
	/** LITERATE_HASKELL */
	LITERATE_HASKELL("Literate Haskell"), //$NON-NLS-1$
	/** LLVM */
	LLVM("LLVM"), //$NON-NLS-1$
	/** LUA */
	LUA("Lua"), //$NON-NLS-1$
	/** MAKEFILE */
	MAKEFILE("Makefile"), //$NON-NLS-1$
	/** MAKO */
	MAKO("Mako"), //$NON-NLS-1$
	/** MARKDOWN */
	MARKDOWN("Markdown"), //$NON-NLS-1$
	/** MATLAB */
	MATLAB("Matlab"), //$NON-NLS-1$
	/** MAX_MSP */
	MAX_MSP("Max/MSP"), //$NON-NLS-1$
	/** MIRAH */
	MIRAH("Mirah"), //$NON-NLS-1$
	/** MOOCODE */
	MOOCODE("Moocode"), //$NON-NLS-1$
	/** MUPAD */
	MUPAD("mupad"), //$NON-NLS-1$
	/** MYGHTY */
	MYGHTY("Myghty"), //$NON-NLS-1$
	/** NIMROD */
	NIMROD("Nimrod"), //$NON-NLS-1$
	/** NU */
	NU("Nu"), //$NON-NLS-1$
	/** NUMPY */
	NUMPY("NumPy"), //$NON-NLS-1$
	/** OBJDUMP */
	OBJDUMP("ObjDump"), //$NON-NLS-1$
	/** OBJECTIVE_C */
	OBJECTIVE_C("Objective-C"), //$NON-NLS-1$
	/** OBJECTIVE_J */
	OBJECTIVE_J("Objective-J"), //$NON-NLS-1$
	/** OCAML */
	OCAML("OCaml"), //$NON-NLS-1$
	/** OOC */
	OOC("ooc"), //$NON-NLS-1$
	/** OPENCL */
	OPENCL("OpenCL"), //$NON-NLS-1$
	/** PARROT_INTERNAL_REPRESENTATION */
	PARROT_INTERNAL_REPRESENTATION("Parrot Internal Representation"), //$NON-NLS-1$
	/** PERL */
	PERL("Perl"), //$NON-NLS-1$
	/** PROLOG */
	PROLOG("Prolog"), //$NON-NLS-1$
	/** PHP */
	PHP("PHP"), //$NON-NLS-1$
	/** PURE_DATA */
	PURE_DATA("Pure Data"), //$NON-NLS-1$
	/** PYTHON */
	PYTHON("Python"), //$NON-NLS-1$
	/** R */
	R("R"), //$NON-NLS-1$
	/** RACKET */
	RACKET("Racket"), //$NON-NLS-1$
	/** RAW_TOKEN_DATA */
	RAW_TOKEN_DATA("Raw token data"), //$NON-NLS-1$
	/** REBOL */
	REBOL("Rebol"), //$NON-NLS-1$
	/** REDCODE */
	REDCODE("Redcode"), //$NON-NLS-1$
	/** RESTRUCTUREDTEXT */
	RESTRUCTUREDTEXT("reStructuredText"), //$NON-NLS-1$
	/** RHTML */
	RHTML("RHTML"), //$NON-NLS-1$
	/** RUBY */
	RUBY("Ruby"), //$NON-NLS-1$
	/** SASS */
	SASS("Sass"), //$NON-NLS-1$
	/** SCALA */
	SCALA("Scala"), //$NON-NLS-1$
	/** SCHEME */
	SCHEME("Scheme"), //$NON-NLS-1$
	/** SELF */
	SELF("Self"), //$NON-NLS-1$
	/** SHELL */
	SHELL("Shell"), //$NON-NLS-1$
	/** SMALLTALK */
	SMALLTALK("Smalltalk"), //$NON-NLS-1$
	/** SMARTY */
	SMARTY("Smarty"), //$NON-NLS-1$
	/** STANDARD_ML */
	STANDARD_ML("Standard ML"), //$NON-NLS-1$
	/** SUPERCOLLIDER */
	SUPERCOLLIDER("SuperCollider"), //$NON-NLS-1$
	/** TCL */
	TCL("Tcl"), //$NON-NLS-1$
	/** TCSH */
	TCSH("Tcsh"), //$NON-NLS-1$
	/** TEX */
	TEX("TeX"), //$NON-NLS-1$
	/** TEXT */
	TEXT("Text"), //$NON-NLS-1$
	/** TEXTILE */
	TEXTILE("Textile"), //$NON-NLS-1$
	/** VALA */
	VALA("Vala"), //$NON-NLS-1$
	/** VERILOG */
	VERILOG("Verilog"), //$NON-NLS-1$
	/** VHDL */
	VHDL("VHDL"), //$NON-NLS-1$
	/** VIML */
	VIML("VimL"), //$NON-NLS-1$
	/** VISUAL_BASIC */
	VISUAL_BASIC("Visual Basic"), //$NON-NLS-1$
	/** XML */
	XML("XML"), //$NON-NLS-1$
	/** XQUERY */
	XQUERY("XQuery"), //$NON-NLS-1$
	/** XS */
	XS("XS"), //$NON-NLS-1$
	/** YAML */
	YAML("YAML"); //$NON-NLS-1$

	private final String value;

	Language(String value) {
		this.value = value;
	}

	/**
	 * Get value
	 *
	 * @return value
	 */
	public String getValue() {
		return value;
	}
}
