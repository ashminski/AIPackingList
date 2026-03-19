package com.ashleykaminski.aipackinglist

import android.content.Context

object DefaultsParser {

    fun parseTemplates(context: Context): List<PackingListTemplate> =
        parseFile(context, "templates.txt").mapIndexed { index, block ->
            val (name, items) = parseBlock(block)
            PackingListTemplate(id = index + 1, name = name, items = items)
        }

    fun parseTopics(context: Context): List<TripTopic> =
        parseFile(context, "topics.txt").mapIndexed { index, block ->
            val (name, items) = parseBlock(block)
            TripTopic(id = index + 1, name = name, items = items)
        }

    fun parseQuestions(context: Context): List<TripQuestion> =
        parseFile(context, "questions.txt").mapIndexed { index, block ->
            val lines = block.trim().lines()
            val text = lines.first().trim()
            val topicNames = lines.drop(1)
                .filter { it.startsWith(">") }
                .flatMap { line ->
                    line.removePrefix(">").trim()
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
            TripQuestion(id = index + 1, text = text, topicNames = topicNames)
        }

    private fun parseFile(context: Context, fileName: String): List<String> =
        context.assets.open(fileName)
            .bufferedReader()
            .readText()
            .trim()
            .split(Regex("\n\n+"))
            .filter { it.isNotBlank() }

    private fun parseBlock(block: String): Pair<String, List<TemplateItem>> {
        val lines = block.trim().lines()
        val name = lines.first().trim()
        val items = lines.drop(1)
            .mapNotNull { line ->
                val text = line.removePrefix("- ").trim()
                text.takeIf { it.isNotEmpty() }
            }
            .mapIndexed { index, text -> TemplateItem(id = index + 1, text = text) }
        return name to items
    }
}
