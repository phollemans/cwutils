#!/bin/sh

##
## NOTE: This script must be run from the doc/ directory.
##

awk=/usr/local/bin/gawk
pdflatex=/usr/texbin/pdflatex
bibtex=/usr/texbin/bibtex
html2latex=/usr/local/bin/html2latex

# Get command line parameters
# ---------------------------
version=$1
if [ -z "$version" ] ; then
  echo "Usage: make_docs.sh version"
  exit 1
fi

# Create tool documentation pages
# -------------------------------
echo "Making tool documentation pages ... \c"
mkdir tools

# Extract HTML from API pages
# ---------------------------
for fname in api/noaa/coastwatch/tools/*.html ; do
  tool=`basename $fname .html`
  grep $tool tool_categories.txt > /dev/null 2>&1
  if [ $? -ne 0 ] ; then
    continue
  fi
  echo "$tool \c"
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
done

# Check for conversion script to Latex
# ------------------------------------
if [ -z `which html2latex` ] ; then
  echo "FAILED (no html2latex)"
  exit
fi

# Create manual pages section in Latex
# ------------------------------------
manual=users_guide/manual_pages.tex
cat > $manual <<EOF
\chapter{Manual Pages}
\label{manual}
EOF
for fname in tools/*.html ; do
  tool=`basename $fname .html`
  $html2latex --border --class=book $fname > /tmp/err$$.txt 2>&1
  if [ $? -ne 0 ] ; then
    echo "FAILED (making LaTeX from $fname)"
    cat /tmp/err$$.txt
    rm -f tools/$tool.tex
    continue
  fi
  $awk '
    BEGIN { 
      document = 0;
      verbatim = 0;
    }
    $0 ~ /\\end{document}/ { document = 0; }
    $0 ~ /\\begin{verbatim}/ { verbatim = 1; }
    $0 ~ /\\end{verbatim}/ { verbatim = 0; }
    $0 ~ /\\begin{tabular}/ { tabular = 1; tabularhead = 1; }
    $0 !~ /\\begin{tabular}/ { tabularhead = 0; }
    $0 ~ /\\end{tabular}/ { tabular = 0; }
    { 
      if (document == 1) {
        if (verbatim == 1) {
          print $0;
        }
        else {
          output = $0;
          output = gensub ("\\\\subsection\\*{(.*)}", "\\\\subsection*{\\\\underline{\\1}}", "g", output);
          output = gensub ("\\\\section\\*{.*: (.*)}", "\\\\section{\\1} \\\\hypertarget{\\1}{}", "g", output);
          if (tabularhead != 1) { 
            output = gensub ("\\|", "$|$", "g", output); 
            if (tabular == 1) {
              output = gensub ("^([^&]+)\\\\\\\\$", "\\1 \\& \\\\\\\\", "g", output);
            }
          }
          output = gensub ("\\-\\-", "-{-}", "g", output);
          output = gensub ("]]", "{]}]", "g", output);
          print output;
        }
      }
    }
    $0 ~ /\\begin{document}/ { document = 1; }
  ' tools/$tool.tex >> $manual
  echo '\\newpage' >> $manual
  rm -f tools/$tool.tex
done  
echo "OK"
rm -rf tools

# Create user guide
# -----------------
echo "Making user's guide ... \c"
cd users_guide
guide_version=`echo $version | sed -e 's/\./_/g'`
guide_name="cwf_users_guide"
guide_name_with_verson="cwf_ug_${guide_version}"
$pdflatex --interaction batchmode ${guide_name}.tex > /dev/null 2>&1
if [ $? -ne 0 ] ; then
  echo "FAILED1"
else
  $bibtex $guide_name > /dev/null 2>&1
  if [ $? -ne 0 ] ; then
    echo "FAILED2"
  else
    $pdflatex --interaction batchmode ${guide_name}.tex > /dev/null 2>&1
    $pdflatex --interaction batchmode ${guide_name}.tex > /dev/null 2>&1
    cp ${guide_name}.pdf ../${guide_name_with_verson}.pdf
    echo "OK"
  fi 
fi