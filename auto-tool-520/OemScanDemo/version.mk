# Hey Emacs, this is a -*- makefile -*-
#########################################################################################
# $Id: $
# Copyright Honeywell International Inc. 2015
#########################################################################################
# Makefile for generating svn revision files
# \file
#########################################################################################
VERSIONXMLTEMPL = svn_rev.xml.tmpl

VERSIONXMLPATH = res/values
VERSIONXMLFILE = $(VERSIONXMLPATH)/svn_rev.xml

#########################################################################################
# Rules
.PHONY : $(VERSIONXMLFILE)
$(VERSIONXMLFILE) : $(VERSIONXMLTEMPL)
	SubWCRev . $< $@
	