<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"> 

<xsl:output method="text" use-character-maps="charmap"/>
<xsl:strip-space elements="*"/>

<!-- Parameters passed in -->

<xsl:param name="tool"/>
<xsl:param name="date"/>
<xsl:param name="package"/>
<xsl:param name="version"/>

<!-- Map these characters -->

<xsl:character-map name="charmap">
  <xsl:output-character character="&#8212;" string="&#xa;.Nd"/>
</xsl:character-map>

<!-- Ignore these tags -->

<xsl:template match="head|h1"></xsl:template>

<!-- Man page header -->
<!-- Note that these formatting command come from the groff_mdoc man page. -->

<xsl:template match="html">
.Dd <xsl:value-of select="$date"/>
.Os "<xsl:value-of select="$package"/>" <xsl:value-of select="$version"/>
.Dt <xsl:value-of select="upper-case($tool)"/> 1 URM
<xsl:apply-templates/>
</xsl:template>

<!-- Section and subsection headers -->

<xsl:template match="h2">
.Sh <xsl:value-of select="upper-case(.)"/>
</xsl:template>

<xsl:template match="h3">
.Ss <xsl:value-of select="."/>
</xsl:template>

<!-- Regular paragraphs -->

<xsl:template match="p">
.Pp
<xsl:apply-templates select="child::node()"/>
</xsl:template>

<xsl:template match="p//text()">
<xsl:value-of select="normalize-space(.)"/>
</xsl:template>

<xsl:template match="text()">
<xsl:value-of select="replace (., '(^[ \n]+|[ \n]+$)', '')"/>
</xsl:template>

<!-- Unordered and ordered lists -->

<xsl:template match="ul">
.Bl -bullet -offset indent
<xsl:apply-templates select="child::node()"/>
.El
</xsl:template>

<xsl:template match="ol">
.Bl -enum -offset indent
<xsl:apply-templates select="child::node()"/>
.El
</xsl:template>

<xsl:template match="li">
.It
<xsl:apply-templates select="child::node()"/>
</xsl:template>

<xsl:template match="li/text()">
<xsl:value-of select="normalize-space(.)"/>
</xsl:template>

<!-- Tables -->

<xsl:template match="table">
.Bl -column -offset indent <xsl:for-each select="tr/th">"Sy <xsl:value-of select="@abbr"/>" </xsl:for-each>
.It <xsl:for-each select="tr/th">Sy <xsl:apply-templates select="child::node()"/> Ta </xsl:for-each>
<xsl:for-each select="tr[td]">
.It <xsl:for-each select="td"><xsl:apply-templates select="child::node()"/> Ta </xsl:for-each>
</xsl:for-each>
.El
</xsl:template>

<!-- Special fonts -->

<xsl:template match="b[following-sibling::text()[matches (string (.), '^[.,:;]')]]">
\fB<xsl:apply-templates select="child::node()"/>\fR</xsl:template>

<xsl:template match="b">
\fB<xsl:apply-templates select="child::node()"/>\fR
</xsl:template>

<xsl:template match="code[following-sibling::text()[matches (string (.), '^[.,:;]')]]">
\fB<xsl:apply-templates select="child::node()"/>\fR</xsl:template>

<xsl:template match="code">
\fB<xsl:apply-templates select="child::node()"/>\fR
</xsl:template>

<xsl:template match="i[following-sibling::text()[matches (string (.), '^[.,:;]')]]">
\fI<xsl:apply-templates select="child::node()"/>\fR</xsl:template>

<xsl:template match="i">
\fI<xsl:apply-templates select="child::node()"/>\fR
</xsl:template>

<xsl:template match="u[following-sibling::text()[matches (string (.), '^[.,:;]')]]">
\fI<xsl:apply-templates select="child::node()"/>\fR</xsl:template>

<xsl:template match="u">
\fI<xsl:apply-templates select="child::node()"/>\fR
</xsl:template>

<!-- Special formatting -->

<xsl:template match="br">
.br
</xsl:template>

<xsl:template match="pre">
.Bd -literal
BLANKLINE
<xsl:value-of select="replace (replace (., '\n\n', '&#xa;BLANKLINE&#xa;'), '\\', '\\\\')"/>
BLANKLINE
.Ed
</xsl:template>

<!-- Definition lists -->

<xsl:template match="dl">
.Bl -tag -width indent
<xsl:apply-templates select="child::node()"/>
.El
</xsl:template>

<xsl:template match="dt">
.It <xsl:apply-templates select="child::node()"/>
<xsl:text>&#xa;</xsl:text>
</xsl:template>

<xsl:template match="dd">
<xsl:apply-templates select="child::node()"/>
</xsl:template>

<xsl:template match="dd//text()">
<xsl:value-of select="normalize-space(.)"/>
</xsl:template>

<xsl:template match="dd/text()[preceding-sibling::node()[1][self::ul]]">
.Pp
<xsl:value-of select="normalize-space(.)"/>
</xsl:template>

</xsl:stylesheet>
