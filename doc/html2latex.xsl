<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="text"/>
<xsl:strip-space elements="*"/>

<!-- Parameters passed in -->

<xsl:param name="tool"/>

<!-- Ignore these tags -->

<xsl:template match="head|h1"></xsl:template>

<!-- Main page header -->

<xsl:template match="html">
\newpage
\section{<xsl:value-of select="$tool"/>} \hypertarget{<xsl:value-of select="$tool"/>}{}
<xsl:apply-templates/>
</xsl:template>

<!-- Section and subsection headers -->

<xsl:template match="h2">
\subsection*{\underline{<xsl:value-of select="."/>}}
</xsl:template>

<xsl:template match="h3">
\subsubsection*{<xsl:value-of select="."/>}
</xsl:template>

<!-- Regular paragraphs -->

<xsl:template match="p">
<xsl:apply-templates select="child::node()"/>
</xsl:template>

<!-- Special characters -->

<xsl:template match="text()">
  <xsl:variable name="sub1" select="replace (., '\|', '\$|\$')"/>
  <xsl:variable name="sub2" select="replace ($sub1, '_', '\\_')"/>
  <xsl:variable name="sub3" select="replace ($sub2, '\{', '\\{')"/>
  <xsl:variable name="sub4" select="replace ($sub3, '\}', '\\}')"/>
  <xsl:variable name="sub5" select="replace ($sub4, '--([^ ]+)', '\\mbox{-{-}$1}')"/>
  <xsl:variable name="sub6" select="replace ($sub5, '&gt;', '\$&gt;\$')"/>
  <xsl:variable name="sub7" select="replace ($sub6, '&lt;', '\$&lt;\$')"/>
  <xsl:variable name="sub8" select="replace ($sub7, '%', '\\%')"/>
  <xsl:variable name="sub9" select="replace ($sub8, '&amp;', '\\&amp;')"/>
  <xsl:variable name="sub10" select="replace ($sub9, '&quot;([^&quot;]+)&quot;', '``$1&apos;&apos;&apos;&apos;')"/>
  <xsl:variable name="sub11" select="replace ($sub10, '\^', '\\^{}')"/>
  <xsl:variable name="sub12" select="replace ($sub11, '\]', '{]}')"/>
  <xsl:variable name="sub13" select="replace ($sub12, '\[', '{[}')"/>
  <xsl:variable name="sub14" select="replace ($sub13, '&#8212;', '--')"/>
  <xsl:value-of select="normalize-space ($sub14)"/>
</xsl:template>

<!-- Unordered and ordered lists -->

<xsl:template match="ul">
\begin{itemize}
<xsl:apply-templates select="child::node()"/>
\end{itemize}
</xsl:template>

<xsl:template match="ol">
\begin{enumerate}
<xsl:apply-templates select="child::node()"/>
\end{enumerate}
</xsl:template>

<xsl:template match="li">
\item <xsl:apply-templates select="child::node()"/>

</xsl:template>

<!-- Tables -->

<xsl:template match="table">

\begin{tabular}{<xsl:for-each select="tr/th">|l</xsl:for-each>|}
\hline
<xsl:for-each select="tr/th[position() &lt; last()]">\textbf{<xsl:apply-templates select="child::node()"/>} &amp; </xsl:for-each>
<xsl:for-each select="tr/th[last()]">\textbf{<xsl:apply-templates select="child::node()"/>} \\ </xsl:for-each>
\hline
<xsl:for-each select="tr[td]">
<xsl:for-each select="td[position() &lt; last()]"><xsl:apply-templates select="child::node()"/> &amp; </xsl:for-each>
<xsl:for-each select="td[last()]"><xsl:apply-templates select="child::node()"/> \\ </xsl:for-each>
\hline
</xsl:for-each>
\end{tabular}

</xsl:template>

<!-- Special fonts -->

<xsl:template match="b[following-sibling::text()[matches (string (.), '^[.,:;]')]]">
\textbf{<xsl:apply-templates select="child::node()"/>}</xsl:template>

<xsl:template match="b">
\textbf{<xsl:apply-templates select="child::node()"/>}
</xsl:template>

<xsl:template match="code[following-sibling::text()[matches (string (.), '^[.,:;]')]]">
{\tt <xsl:apply-templates select="child::node()"/>}</xsl:template>

<xsl:template match="code">
{\tt <xsl:apply-templates select="child::node()"/>}
</xsl:template>

<xsl:template match="i[following-sibling::text()[matches (string (.), '^[.,:;]')]]">
\emph{<xsl:apply-templates select="child::node()"/>}</xsl:template>

<xsl:template match="i">
\emph{<xsl:apply-templates select="child::node()"/>}
</xsl:template>

<xsl:template match="u[following-sibling::text()[matches (string (.), '^[.,:;]')]]">
\underline{<xsl:apply-templates select="child::node()"/>}</xsl:template>

<xsl:template match="u">
\underline{<xsl:apply-templates select="child::node()"/>}
</xsl:template>

<!-- Special formatting -->

<xsl:template match="br">\\
</xsl:template>

<xsl:template match="pre">
\begin{verbatim}
<xsl:value-of select="."/>
\end{verbatim}
</xsl:template>

<!-- Definition lists -->

<xsl:template match="dl">
\begin{description}
<xsl:apply-templates select="child::node()"/>
\end{description}
</xsl:template>

<xsl:template match="dt">
\item[<xsl:apply-templates select="child::node()"/>] </xsl:template>
<xsl:template match="dd"><xsl:apply-templates select="child::node()"/></xsl:template>

<xsl:template match="a">
\href{<xsl:value-of select="@href"/>}{<xsl:apply-templates select="child::node()"/>}
</xsl:template>

</xsl:stylesheet>
