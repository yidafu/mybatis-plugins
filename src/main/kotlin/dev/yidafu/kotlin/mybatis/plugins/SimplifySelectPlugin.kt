package dev.yidafu.kotlin.mybatis.plugins

import org.mybatis.generator.api.IntrospectedTable
import org.mybatis.generator.api.PluginAdapter
import org.mybatis.generator.api.dom.kotlin.*
import org.mybatis.generator.internal.util.JavaBeansUtil
import java.sql.JDBCType

class SimplifySelectPlugin : PluginAdapter() {
    /**
     * @param warnings add strings to this list to specify warnings. For example, if
     * the plugin is invalid, you should specify why. Warnings are
     * reported to users after the completion of the run.
     * @return
     */
    override fun validate(warnings: List<String>): Boolean {
        System.out.println("SimplifySelectPlugin")
        return true
    }

    override fun clientColumnListPropertyGenerated(
        kotlinProperty: KotlinProperty?,
        kotlinFile: KotlinFile?,
        introspectedTable: IntrospectedTable?
    ): Boolean {
        val fieldInitializer = introspectedTable?.allColumns?.filter {
            it.jdbcType != JDBCType.LONGVARCHAR.vendorTypeNumber &&
                    it.jdbcType != JDBCType.LONGNVARCHAR.vendorTypeNumber &&
                    it.jdbcType != JDBCType.LONGVARBINARY.vendorTypeNumber
        }?.map { it.javaProperty }?.joinToString(", ", "listOf(", ")")
        fieldInitializer?.let {
            val prop = KotlinProperty
                .newVal("simplifyColumnList")
                .withModifier(KotlinModifier.PRIVATE)
                .withInitializationString(fieldInitializer)
                .build()
            kotlinFile?.addNamedItem(prop)
        }
        return super.clientColumnListPropertyGenerated(kotlinProperty, kotlinFile, introspectedTable)
    }

    override fun clientBasicSelectManyMethodGenerated(
        kotlinFunction: KotlinFunction?,
        kotlinFile: KotlinFile?,
        introspectedTable: IntrospectedTable?
    ): Boolean {
        val mapperName = introspectedTable?.tableConfiguration?.mapperName
        // @see org/mybatis/generator/runtime/kotlin/KotlinDynamicSqlSupportClassGenerator.java:137
        val tableFieldName = JavaBeansUtil.getValidPropertyName(introspectedTable?.tableConfiguration?.domainObjectName)
        val func = KotlinFunction
            .newOneLineFunction("$mapperName.selectSimplify")
            .withArgument(
                KotlinArg
                    .newArg("completer")
                    .withDataType("SelectCompleter")
                    .build()
            ).withCodeLine("selectList(this::selectMany, simplifyColumnList, ${tableFieldName}, completer)").build()

        introspectedTable?.context?.commentGenerator?.addGeneralFunctionComment(func, introspectedTable, mutableSetOf())
        kotlinFile?.addNamedItem(func)
        return super.clientBasicSelectManyMethodGenerated(kotlinFunction, kotlinFile, introspectedTable)
    }

}