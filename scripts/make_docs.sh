#!/bin/sh

##
## NOTE: This script must be run from the doc/ directory.
##

awk=/opt/local/bin/gawk
pdflatex=/opt/local/bin/pdflatex
bibtex=/opt/local/bin/bibtex
saxon_jar=../scripts/saxon9he.jar
saxon="java -cp ${saxon_jar} net.sf.saxon.Transform"

# Get command line parameters
# ---------------------------
version=$1
if [ -z "$version" ] ; then
  echo "Usage: make_docs.sh version"
  exit 1
fi

# Create tool documentation pages
# -------------------------------
echo "Making tool HTML pages"
mkdir tools

# Extract HTML from API pages
# ---------------------------
for fname in api/noaa/coastwatch/tools/*.html ; do
  tool=`basename $fname .html`
  grep $tool tool_categories.txt > /dev/null 2>&1
  if [ $? -ne 0 ] ; then
    continue
  fi
  echo "--> $tool"
  newfname=tools/$tool.html
  cat > $newfname <<EOF
<html>

<head>
  <title>CoastWatch Software Library and Utilities v$version: $tool</title>
  <link rel="stylesheet" href="../stylesheet.css" type="text/css" />
</head>

<body>

<h1>CoastWatch Software Library and Utilities v$version: $tool</h1>

EOF
  $awk '
    BEGIN { copy = 0; }
    $0 ~ /END MAN PAGE/ { copy = 0; }
    { if (copy == 1) print $0 }
    $0 ~ /START MAN PAGE/ { copy = 1; }
  ' $fname >> $newfname
  cat >> $newfname <<EOF

</body>

</html>
EOF
  sed -e 's/<br>/<br\/>/g' $newfname > $newfname.new
  mv $newfname.new $newfname
done

# Check for JRE for next part
# ---------------------------
if [ -z `which java` ] ; then
  echo "No Java runtime detected, stopping"
  exit
fi

# Create manual pages section in Latex
# ------------------------------------
echo "Making tool Latex pages"
manual=users_guide/manual_pages.tex
cat > $manual <<EOF
\chapter{Manual Pages}
\label{manual}
EOF
for fname in tools/*.html ; do
  tool=`basename $fname .html`
  echo "--> $tool"
  $saxon \
    tool="$tool" \
    $fname html2latex.xsl > tools/$tool.tex 2> /tmp/err$$.txt
  if [ $? -ne 0 ] ; then
    echo "Error making Latex from ${fname}, message as follows:"
    cat /tmp/err$$.txt
    continue
  fi
  cat tools/$tool.tex >> $manual
done

# Create manual pages in Unix mdoc format
# ---------------------------------------
echo "Making tool Unix man pages"
mkdir -p man/man1
date="`date +'%b %e, %Y'`"
package="CoastWatch Utilities"
for fname in tools/*.html ; do
  tool=`basename $fname .html`
  echo "--> $tool"
  $saxon \
    tool="$tool" \
    date="$date" \
    package="$package" \
    version="$version" \
    $fname html2mdoc.xsl > man/man1/$tool.1 2> /tmp/err$$.txt
  if [ $? -ne 0 ] ; then
    echo "Error making man page from ${fname}, message as follows:"
    cat /tmp/err$$.txt
    rm -f man/man1/$tool.1
    continue
  fi
  awk '{if ($0 == "BLANKLINE") {print ""} else if ($0 !~ /^[ ]*$/) {print}}' man/man1/$tool.1 | gzip -c > man/man1/$tool.1.gz
  rm -f man/man1/$tool.1
done

# Comment this out for debugging
rm -rf tools

# Create user guide
# -----------------
echo "Making user's guide"
cd users_guide
guide_version=`echo $version | sed -e 's/\./_/g'`
guide_name="cwutils_users_guide"
guide_name_with_verson="cwutils_ug_${guide_version}"
$pdflatex --interaction batchmode ${guide_name}.tex > /dev/null 2>&1
if [ $? -ne 0 ] ; then
  echo "Error running pdflatex"
else
  $bibtex $guide_name > /dev/null 2>&1
  if [ $? -ne 0 ] ; then
    echo "Error running bibtex"
  else
    $pdflatex --interaction batchmode ${guide_name}.tex > /dev/null 2>&1
    $pdflatex --interaction batchmode ${guide_name}.tex > /dev/null 2>&1
    cp ${guide_name}.pdf ../${guide_name_with_verson}.pdf
    echo "Copied finished user's guide to ${guide_name_with_verson}.pdf"
  fi
fi
