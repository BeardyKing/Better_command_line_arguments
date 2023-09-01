package com.github.beardyking.bettercommandlineargs.toolWindow
//
//import com.github.beardyking.bettercommandlineargs.MyBundle
//import com.github.beardyking.bettercommandlineargs.services.MyProjectService
//import com.intellij.openapi.components.service
//import com.intellij.openapi.diagnostic.thisLogger
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.wm.ToolWindow
//import com.intellij.openapi.wm.ToolWindowFactory
//import com.intellij.ui.components.JBLabel
//import com.intellij.ui.components.JBPanel
//import com.intellij.ui.content.ContentFactory
//import javax.swing.JButton
//
//data class Argument(val inActive: Boolean, val inCommand: String) {
//    val active = inActive;
//    val command = inCommand;
//}
//
//class Node1() {
//    var data: String = "";
//    var nodes: ArrayList<Node> = ArrayList<Node>();
//}
//
//
//class Node(n: Int, var data: Int) {
//    // List of children
//    var children: Array<Node?>
//
//    init {
//        children = arrayOfNulls(n)
//    }
//}
//
//fun inorder(node: Node?, builder: StringBuilder) {
//    if (node == null) {
//        return;
//    }
//
//    val total = node.children.size
//
//    for (i in 0 until total - 1) {
//        inorder(node.children[i], builder);
//    }
//
//    builder.append(node.data);// += toString();
//    inorder(node.children[total - 1], builder)
//}
//
//class MyToolWindowFactory : ToolWindowFactory {
//
//    init {
//        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
//    }
//
//    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
//        val myToolWindow = MyToolWindow(toolWindow)
//        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
//        toolWindow.contentManager.addContent(content)
//    }
//
//    override fun shouldBeAvailable(project: Project) = true
//
//    class MyToolWindow(toolWindow: ToolWindow) {
//
//        private val service = toolWindow.project.service<MyProjectService>()
//
//        fun getContent() = JBPanel<JBPanel<*>>().apply {
//            val label = JBLabel(MyBundle.message("randomLabel", "?"))
//
//
//            add(label)
//            add(JButton(MyBundle.message("shuffle")).apply {
//                addActionListener {
//                    label.text = MyBundle.message("randomLabel", service.getRandomNumber())
//                }
//            })
//
////            val n = 3
////            val root = Node(n, 1)
////            root.children[0] = Node(n, 2)
////            root.children[1] = Node(n, 3)
////            root.children[2] = Node(n, 4)
////            root.children[0]!!.children[0] = Node(n, 5)
////            root.children[0]!!.children[1] = Node(n, 6)
////            root.children[0]!!.children[2] = Node(n, 7)
////
////            val builder: StringBuilder = StringBuilder();
////            inorder(root, builder);
////            val parsedArgs = JBLabel(MyBundle.message(builder.toString()))
//////            add(parsedArgs);
//        }
//    }
//}
