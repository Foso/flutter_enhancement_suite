package de.mariushoefler.flutter_enhancement_suite.pub

import com.intellij.codeInsight.daemon.GroupNames
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import de.mariushoefler.flutter_enhancement_suite.utils.FileParser
import kotlinx.coroutines.runBlocking

class PubPackagesInspection : LocalInspectionTool() {

	private val dependencyChecker = DependencyChecker()

	override fun getDisplayName() = "Pub Packages latest versions"

	override fun getGroupDisplayName(): String = GroupNames.DEPENDENCY_GROUP_NAME

	override fun getShortName() = "PubVersions"

	override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
		return YamlElementVisitor(holder, isOnTheFly, dependencyChecker)
	}

	override fun isEnabledByDefault() = true
}

class YamlElementVisitor(
		private val holder: ProblemsHolder,
		private val isOnTheFly: Boolean,
		private val dependencyChecker: DependencyChecker
) : PsiElementVisitor() {

	override fun visitFile(file: PsiFile) {
		if (!isOnTheFly) return

		runBlocking {
			val fileParser = FileParser(file, dependencyChecker)
			val problemDescriptions = fileParser.checkFile()

			problemDescriptions.forEach {
				holder.showProblem(file, it.counter, it.latestVersion)
			}
		}
	}
}

private fun ProblemsHolder.showProblem(
		file: PsiFile,
		counter: Int,
		latestVersion: String
) {
	val psiElement = file.findElementAt(counter)!!
	registerProblem(
			psiElement,
			"There's a new version available: $latestVersion",
			DependencyQuickFix(psiElement, latestVersion)
	)
}