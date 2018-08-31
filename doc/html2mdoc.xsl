<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"> 

<xsl:output method="text" use-character-maps="charmap"/>
<xsl:strip-space elements="*"/>

<!-- Parameters passed in -->

<xsl:param name="tool"/>
<xsl:param name="date"/>
<xsl:param name="package"/>
<xsl:param name="version"/>

<!-- Ignore these tags -->

<xsl:template match="head|h1">
</xsl:template>

<!-- Man page header -->
<!-- Note that these formatting command come from the groff_mdoc man page. -->

<xsl:template match="html">

  <xsl:text>&#xa;.Dd </xsl:text>
  <xsl:value-of select="$date"/>

  <xsl:text>&#xa;.Os "</xsl:text>
  <xsl:value-of select="$package"/>
  <xsl:text>" </xsl:text>
  <xsl:value-of select="$version"/>

  <xsl:text>&#xa;.Dt </xsl:text>
  <xsl:value-of select="upper-case($tool)"/>
  <xsl:text> 1 URM&#xa;</xsl:text>
 
  <xsl:apply-templates/>

</xsl:template>

<!-- Section and subsection headers -->

<xsl:template match="h2">
  <xsl:text>&#xa;.Sh </xsl:text>
  <xsl:value-of select="upper-case(.)"/>
  <xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="h3">
  <xsl:text>&#xa;.Ss </xsl:text>
  <xsl:value-of select="."/>
  <xsl:text>&#xa;</xsl:text>
</xsl:template>

<!-- Regular paragraphs -->

<xsl:template match="p">
  <xsl:text>&#xa;.Pp&#xa;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#xa;</xsl:text>
</xsl:template>

<!-- Replace special characters -->

<xsl:character-map name="charmap">

  <!-- Transform (emdash) to (newline).Nd -->
  <xsl:output-character character="&#8212;" string="&#xa;.Nd"/>

</xsl:character-map>

<!-- Unordered and ordered lists -->

<xsl:template match="ul">
  <xsl:text>&#xa;.Bl -bullet -offset indent&#xa;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#xa;.El&#xa;</xsl:text>
</xsl:template>

<xsl:template match="ol">
  <xsl:text>&#xa;.Bl -enum -offset indent&#xa;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#xa;.El&#xa;</xsl:text>
</xsl:template>

<xsl:template match="li">
  <xsl:text>&#xa;.It&#xa;</xsl:text>
  <xsl:apply-templates/>
</xsl:template>

<!-- Tables -->

<xsl:template match="table">

  <xsl:text>&#xa;.Bl -column -offset indent </xsl:text>
  <xsl:for-each select="tr/th">
    <xsl:text>"Sy </xsl:text>
    <xsl:value-of select="@abbr"/>
    <xsl:text>" </xsl:text>
  </xsl:for-each>
  <xsl:text>&#xa;</xsl:text>
  
  <xsl:text>.It </xsl:text>
  <xsl:for-each select="tr/th">
    <xsl:text>Sy </xsl:text>
    <xsl:apply-templates/>
    <xsl:text> Ta </xsl:text>
  </xsl:for-each>
  <xsl:text>&#xa;</xsl:text>

  <xsl:for-each select="tr[td]">
    <xsl:text>.It </xsl:text>
    <xsl:for-each select="td">
      <xsl:apply-templates/>
      <xsl:text> Ta </xsl:text>
    </xsl:for-each>
    <xsl:text>&#xa;</xsl:text>
  </xsl:for-each>

  <xsl:text>.El&#xa;</xsl:text>

</xsl:template>

<!-- Special fonts -->

<xsl:template match="b">
  <xsl:text>\fB</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>\fR</xsl:text>
</xsl:template>

<xsl:template match="code">
  <xsl:text>\fI</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>\fR</xsl:text>
</xsl:template>

<xsl:template match="i">
  <xsl:text>\fI</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>\fR</xsl:text>
</xsl:template>

<xsl:template match="u">
  <xsl:text>\fI</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>\fR</xsl:text>
</xsl:template>

<!-- Special formatting -->

<xsl:template match="br">
  <xsl:text>&#xa;.br&#xa;</xsl:text>
</xsl:template>

<xsl:template match="pre">
  <xsl:text>&#xa;.Bd -literal&#xa;</xsl:text>
  <xsl:value-of select="replace (., '\\', '\\\\')"/>
  <xsl:text>&#xa;.Ed&#xa;</xsl:text>
</xsl:template>

<!-- Definition lists -->

<xsl:template match="dl">
  <xsl:text>&#xa;.Bl -tag -width indent&#xa;</xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#xa;.El&#xa;</xsl:text>
</xsl:template>

<xsl:template match="dt">
  <xsl:text>&#xa;.It </xsl:text>
  <xsl:apply-templates/>
  <xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="dd">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="dd/text()[preceding-sibling::node()[1][self::ul]]">
.Pp
<xsl:value-of select="."/>
</xsl:template>

</xsl:stylesheet>
