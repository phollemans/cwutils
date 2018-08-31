<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="text"/>
<xsl:strip-space elements="*"/>

<!-- Parameters passed in -->

<xsl:param name="tool"/>

<!-- Ignore these tags -->

<xsl:template match="head|h1">
</xsl:template>

<!-- Main page header -->

<xsl:template match="html">
  <xsl:text>\subsection{</xsl:text>
  <xsl:value-of select="$tool"/>
  <xsl:text>} \hypertarget{</xsl:text>
  <xsl:value-of select="$tool"/>
  <xsl:text>}{}&#xa;</xsl:text>
  <xsl:apply-templates/>
</xsl:template>

<!-- Section and subsection headers -->

<xsl:template match="h2">
  <xsl:text>\subsubsection*{\underline{</xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>}}&#xa;</xsl:text>
</xsl:template>

<xsl:template match="h3">
  <xsl:text>\subsubsection*{</xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>}&#xa;</xsl:text>
</xsl:template>

<!-- Regular paragraphs -->

<xsl:template match="p">
  <xsl:text>\par </xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#xa;</xsl:text>
</xsl:template>

<!-- Replace special characters -->

<xsl:template match="text()">
  <xsl:variable name="result" select="."/>

  <!-- Transform | to $|$ -->
  <xsl:variable name="result" select="replace ($result, '\|', '\$|\$')"/>

  <!-- Transform _ to \_ -->
  <xsl:variable name="result" select="replace ($result, '_', '\\_')"/>

  <!-- Transform { to \{ -->
  <xsl:variable name="result" select="replace ($result, '\{', '\\{')"/>

  <!-- Transform } to \} -->
  <xsl:variable name="result" select="replace ($result, '\}', '\\}')"/>

  <!-- Transform (dash)(dash)WORD to \mbox{-{-}WORD} -->
  <xsl:variable name="result" select="replace ($result, '--([^ ]+)', '\\mbox{-{-}$1}')"/>

  <!-- Transform > to $>$ -->
  <xsl:variable name="result" select="replace ($result, '&gt;', '\$&gt;\$')"/>

  <!-- Transform < to $<$ -->
  <xsl:variable name="result" select="replace ($result, '&lt;', '\$&lt;\$')"/>

  <!-- Transform % to \% -->
  <xsl:variable name="result" select="replace ($result, '%', '\\%')"/>

  <!-- Transform % to \% -->
  <xsl:variable name="result" select="replace ($result, '&amp;', '\\&amp;')"/>

  <!-- Transform "WORD" to ``WORD'' -->
  <xsl:variable name="result" select="replace ($result, '&quot;([^&quot;]+)&quot;', '``$1&apos;&apos;&apos;&apos;')"/>

  <!-- Transform ^ to \^{} -->
  <xsl:variable name="result" select="replace ($result, '\^', '\\^{}')"/>

  <!-- Transform ] to {]} -->
  <xsl:variable name="result" select="replace ($result, '\]', '{]}')"/>

  <!-- Transform [ to {[} -->
  <xsl:variable name="result" select="replace ($result, '\[', '{[}')"/>

  <!-- Transform (emdash) to (dash)(dash) -->
  <xsl:variable name="result" select="replace ($result, '&#8212;', '--')"/>

  <!-- Transform ~ to \textasciitilde{} -->
  <xsl:variable name="result" select="replace ($result, '&#126;', '\\textasciitilde{}')"/>

  <xsl:value-of select="$result"/>
</xsl:template>

<!-- Unordered and ordered lists -->

<xsl:template match="ul">
  <xsl:text>\begin{itemize}&#xa;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>\end{itemize}&#xa;</xsl:text>
</xsl:template>

<xsl:template match="ol">
  <xsl:text>\begin{enumerate}&#xa;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>\end{enumerate}&#xa;</xsl:text>
</xsl:template>

<xsl:template match="li">
  <xsl:text>\item </xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#xa;</xsl:text>
</xsl:template>

<!-- Tables -->

<xsl:template match="table">

  <xsl:text>\begin{tabular}{</xsl:text>
  <xsl:for-each select="tr/th">
    <xsl:text>|l</xsl:text>
  </xsl:for-each>
  <xsl:text>|}&#xa;</xsl:text>
  <xsl:text>\hline&#xa;</xsl:text>

  <xsl:for-each select="tr/th[position() &lt; last()]">
    <xsl:text>\textbf{</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>} &amp; </xsl:text>
  </xsl:for-each>

  <xsl:for-each select="tr/th[last()]">
    <xsl:text>\textbf{</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>} \\&#xa;</xsl:text>
  </xsl:for-each>

  <xsl:text>\hline&#xa;</xsl:text>

  <xsl:for-each select="tr[td]">

    <xsl:for-each select="td[position() &lt; last()]">
      <xsl:apply-templates/>
      <xsl:text> &amp; </xsl:text>
    </xsl:for-each>

    <xsl:for-each select="td[last()]">
      <xsl:apply-templates/>
      <xsl:text> \\&#xa;</xsl:text>
    </xsl:for-each>

  </xsl:for-each>

  <xsl:text>\hline&#xa;</xsl:text>
  <xsl:text>\end{tabular}&#xa;</xsl:text>

</xsl:template>

<!-- Special fonts -->

<xsl:template match="b">
  <xsl:text>\textbf{</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}</xsl:text>
</xsl:template>

<xsl:template match="code">
  <xsl:text>{\tt </xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}</xsl:text>
</xsl:template>

<xsl:template match="i">
  <xsl:text>\emph{</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}</xsl:text>
</xsl:template>

<xsl:template match="u">
  <xsl:text>\underline{</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}</xsl:text>
</xsl:template>

<!-- Special formatting -->

<xsl:template match="br">
  <xsl:text>\\</xsl:text>
</xsl:template>

<xsl:template match="pre">
  <xsl:text>\begin{verbatim}</xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>\end{verbatim}&#xa;</xsl:text>
</xsl:template>

<!-- Definition lists -->

<xsl:template match="dl">
  <xsl:text>\begin{description}&#xa;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>\end{description}&#xa;</xsl:text>
</xsl:template>

<xsl:template match="dt">
  <xsl:text>\item[</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>] </xsl:text>
</xsl:template>

<xsl:template match="dd">
  <xsl:apply-templates/>
  <xsl:text>&#xa;</xsl:text>
</xsl:template>

<!-- Hypertext references -->

<xsl:template match="a">
  <xsl:text>\href{</xsl:text>
  <xsl:value-of select="@href"/>
  <xsl:text>}{</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>}</xsl:text>
</xsl:template>

</xsl:stylesheet>
