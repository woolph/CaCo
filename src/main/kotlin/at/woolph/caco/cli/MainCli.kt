package at.woolph.caco.cli

import org.jline.builtins.Commands
import org.jline.builtins.Completers
import org.jline.builtins.Completers.RegexCompleter
import org.jline.builtins.Completers.TreeCompleter
import org.jline.builtins.Options.HelpException
import org.jline.builtins.TTop
import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.MaskingCallback
import org.jline.reader.ParsedLine
import org.jline.reader.Parser
import org.jline.reader.UserInterruptException
import org.jline.reader.Widget
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.LineReaderImpl
import org.jline.reader.impl.completer.ArgumentCompleter
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.jline.utils.Display
import org.jline.utils.InfoCmp
import org.jline.utils.Status
import java.io.IOException
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

fun main(args: Array<String>) {
    val printer = System.out
    val reader = System.`in`

    printer.printf("shell> ")

    val currentCommand = StringBuffer()

    val input = reader.read()
    if (input == Key.UP) {
        printer.printf("\r")
        printer.printf("shell> history back")
    }

}

object Key {
    val UP = 0
}

/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */



object Example {
    fun usage() {
        val usage = arrayOf(
            "Usage: java " + Example::class.java.name + " [cases... [trigger mask]]",
            "  Terminal:",
            "    -system          terminalBuilder.system(false)",
            "    +system          terminalBuilder.system(true)",
            "  Completors:",
            "    argument          an argument completor",
            "    files            a completor that completes file names",
            "    none             no completors",
            "    param            a paramenter completer using Java functional interface",
            "    regexp           a regex completer",
            "    simple           a string completor that completes \"foo\", \"bar\" and \"baz\"",
            "    tree             a tree completer",
            "  Multiline:",
            "    brackets         eof on unclosed bracket",
            "    quotes           eof on unclosed quotes",
            "  Mouse:",
            "    mouse            enable mouse",
            "    mousetrack       enable tracking mouse",
            "  Miscellaneous:",
            "    color            colored left and right prompts",
            "    status           multi-thread test of jline status line",
            "    timer            widget 'Hello world'",
            "    <trigger> <mask> password mask",
            "  Example:",
            "    java " + Example::class.java.name + " simple su '*'"
        )
        for (u in usage) {
            println(u)
        }
    }

    fun help() {
        val help = arrayOf(
            "List of available commands:",
            "  Builtin:",
            "    history    list history of commands",
            "    less       file pager",
            "    nano       nano editor",
            "    setopt     set options",
            "    ttop       display and update sorted information about threads",
            "    unsetopt   unset options",
            "  Example:",
            "    cls        clear screen",
            "    help       list available commands",
            "    exit       exit from example app",
            "    select     select option",
            "    set        set lineReader variable",
            "    sleep      sleep 3 seconds",
            "    testkey    display key events",
            "    tput       set terminal capability",
            "  Additional help:",
            "    <command> --help"
        )
        for (u in help) {
            println(u)
        }
    }

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            var prompt: String? = "prompt> "
            var rightPrompt: String? = null
            var mask: Char? = null
            var trigger: String? = null
            var color = false
            var timer = false
            val builder = TerminalBuilder.builder()
            if (args.isEmpty()) {
                usage()
                return
            }
            var mouse = 0
            var completer: Completer? = null
            var parser: Parser? = null
            val callbacks: MutableList<Consumer<LineReader>> = ArrayList()
            var index = 0
            while (index < args.size) {
                when (args[index]) {
                    "timer" -> timer = true
                    "-system" -> builder.system(false).streams(System.`in`, System.out)
                    "+system" -> builder.system(true)
                    "none" -> {}
                    "files" -> completer = Completers.FileNameCompleter()
                    "simple" -> {
                        val p3 = DefaultParser()
                        p3.escapeChars = charArrayOf()
                        parser = p3
                        completer = StringsCompleter("foo", "bar", "baz", "pip pop")
                    }

                    "quotes" -> {
                        val p = DefaultParser()
                        p.isEofOnUnclosedQuote = true
                        parser = p
                    }

                    "brackets" -> {
                        prompt = "long-prompt> "
                        val p2 = DefaultParser()
                        p2.setEofOnUnclosedBracket(
                            DefaultParser.Bracket.CURLY,
                            DefaultParser.Bracket.ROUND,
                            DefaultParser.Bracket.SQUARE
                        )
                        parser = p2
                    }

                    "status" -> {
                        completer = StringsCompleter("foo", "bar", "baz")
                        callbacks.add(Consumer { reader: LineReader ->
                            val thread = Thread {
                                var counter = 0
                                while (true) {
                                    try {
                                        val status =
                                            Status.getStatus(reader.terminal)
                                        counter++
                                        status.update(
                                            Arrays.asList(
                                                AttributedStringBuilder()
                                                    .append("counter: $counter")
                                                    .toAttributedString()
                                            )
                                        )
                                        (reader as LineReaderImpl).redisplay()
                                        Thread.sleep(1000)
                                    } catch (e: InterruptedException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            thread.isDaemon = true
                            thread.start()
                        })
                    }

                    "argument" -> completer = ArgumentCompleter(
                        StringsCompleter("foo11", "foo12", "foo13"),
                        StringsCompleter("foo21", "foo22", "foo23"),
                        Completer { reader, line, candidates ->
                            candidates.add(
                                Candidate(
                                    "",
                                    "",
                                    null,
                                    "frequency in MHz",
                                    null,
                                    null,
                                    false
                                )
                            )
                        })

                    "param" -> completer =
                        Completer { reader: LineReader?, line: ParsedLine, candidates: MutableList<Candidate?> ->
                            if (line.wordIndex() == 0) {
                                candidates.add(Candidate("Command1"))
                            } else if (line.words()[0] == "Command1") {
                                if (line.words()[line.wordIndex() - 1] == "Option1") {
                                    candidates.add(Candidate("Param1"))
                                    candidates.add(Candidate("Param2"))
                                } else {
                                    if (line.wordIndex() == 1) {
                                        candidates.add(Candidate("Option1"))
                                    }
                                    if (!line.words().contains("Option2")) {
                                        candidates.add(Candidate("Option2"))
                                    }
                                    if (!line.words().contains("Option3")) {
                                        candidates.add(Candidate("Option3"))
                                    }
                                }
                            }
                        }

                    "tree" -> completer = TreeCompleter(
                        TreeCompleter.node(
                            "Command1",
                            TreeCompleter.node(
                                "Option1",
                                TreeCompleter.node("Param1", "Param2")
                            ),
                            TreeCompleter.node("Option2"),
                            TreeCompleter.node("Option3")
                        )
                    )

                    "regexp" -> {
                        val comp: MutableMap<String, Completer> = HashMap()
                        comp["C1"] = StringsCompleter("cmd1")
                        comp["C11"] = StringsCompleter("--opt11", "--opt12")
                        comp["C12"] = StringsCompleter("arg11", "arg12", "arg13")
                        comp["C2"] = StringsCompleter("cmd2")
                        comp["C21"] = StringsCompleter("--opt21", "--opt22")
                        comp["C22"] = StringsCompleter("arg21", "arg22", "arg23")
                        completer = RegexCompleter(
                            "C1 C11* C12+ | C2 C21* C22+"
                        ) { key: String ->
                            comp[key]
                        }
                    }

                    "color" -> {
                        color = true
                        prompt = AttributedStringBuilder()
                            .style(AttributedStyle.DEFAULT.background(AttributedStyle.GREEN))
                            .append("foo")
                            .style(AttributedStyle.DEFAULT)
                            .append("@bar")
                            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                            .append("\nbaz")
                            .style(AttributedStyle.DEFAULT)
                            .append("> ").toAnsi()
                        rightPrompt = AttributedStringBuilder()
                            .style(AttributedStyle.DEFAULT.background(AttributedStyle.RED))
                            .append(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                            .append("\n")
                            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED or AttributedStyle.BRIGHT))
                            .append(
                                LocalTime.now().format(
                                    DateTimeFormatterBuilder()
                                        .appendValue(ChronoField.HOUR_OF_DAY, 2)
                                        .appendLiteral(':')
                                        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                                        .toFormatter()
                                )
                            )
                            .toAnsi()
                        completer = StringsCompleter("\u001B[1mfoo\u001B[0m", "bar", "\u001B[32mbaz\u001B[0m", "foobar")
                    }

                    "mouse" -> mouse = 1
                    "mousetrack" -> mouse = 2
                    else -> if (index == 0) {
                        usage()
                        return
                    } else if (args.size == index + 2) {
                        mask = args[index + 1][0]
                        trigger = args[index]
                        index = args.size
                    } else {
                        println("Bad test case: " + args[index])
                    }
                }
                index++
            }
            val terminal = builder.build()
            println(terminal.name + ": " + terminal.type)
            println("\nhelp: list available commands")
            val reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(completer)
                .parser(parser)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                .variable(LineReader.INDENTATION, 2)
                .option(LineReader.Option.INSERT_BRACKET, true)
                .build()
            if (timer) {
                Executors.newScheduledThreadPool(1)
                    .scheduleAtFixedRate({
                        reader.callWidget(LineReader.CLEAR)
                        reader.terminal.writer().println("Hello world!")
                        reader.callWidget(LineReader.REDRAW_LINE)
                        reader.callWidget(LineReader.REDISPLAY)
                        reader.terminal.writer().flush()
                    }, 1, 1, TimeUnit.SECONDS)
            }
            if (mouse != 0) {
                reader.setOpt(LineReader.Option.MOUSE)
                if (mouse == 2) {
                    reader.widgets[LineReader.CALLBACK_INIT] = Widget {
                        terminal.trackMouse(Terminal.MouseTracking.Any)
                        true
                    }
                    reader.widgets[LineReader.MOUSE] = Widget {
                        val event = reader.readMouseEvent()
                        val tsb = StringBuilder()
                        val cursor =
                            terminal.getCursorPosition { c: Int ->
                                tsb.append(
                                    c.toChar()
                                )
                            }
                        reader.runMacro(tsb.toString())
                        val msg = "          $event"
                        val w = terminal.width
                        terminal.puts(
                            InfoCmp.Capability.cursor_address,
                            0,
                            Math.max(0, w - msg.length)
                        )
                        terminal.writer().append(msg)
                        terminal.puts(InfoCmp.Capability.cursor_address, cursor.y, cursor.x)
                        terminal.flush()
                        true
                    }
                }
            }
            callbacks.forEach(Consumer { c: Consumer<LineReader> ->
                c.accept(
                    reader
                )
            })
            if (!callbacks.isEmpty()) {
                Thread.sleep(2000)
            }
            val printAbove = AtomicBoolean()
            while (true) {
                var line: String? = null
                try {
                    line = reader.readLine(prompt, rightPrompt, null as MaskingCallback?, null)
                    line = line.trim { it <= ' ' }
                    if (color) {
                        terminal.writer().println(
                            AttributedString.fromAnsi("\u001B[33m======>\u001B[0m\"$line\"")
                                .toAnsi(terminal)
                        )
                    } else {
                        terminal.writer().println("======>\"$line\"")
                    }
                    terminal.flush()

                    // If we input the special word then we will mask
                    // the next line.
                    if (trigger != null && line.compareTo(trigger) == 0) {
                        line = reader.readLine("password> ", mask)
                    }
                    if (line.equals("quit", ignoreCase = true) || line.equals("exit", ignoreCase = true)) {
                        break
                    }
                    val pl = reader.parser.parse(line, 0)
                    val argv = pl.words().subList(1, pl.words().size).toTypedArray()
                    if ("printAbove" == pl.word()) {
                        if (pl.words().size == 2) {
                            if ("start" == pl.words()[1]) {
                                printAbove.set(true)
                                val t = Thread {
                                    try {
                                        var i = 0
                                        while (printAbove.get()) {
                                            reader.printAbove("Printing line " + ++i + " above")
                                            Thread.sleep(1000)
                                        }
                                    } catch (t2: InterruptedException) {
                                    }
                                }
                                t.isDaemon = true
                                t.start()
                            } else if ("stop" == pl.words()[1]) {
                                printAbove.set(false)
                            } else {
                                terminal.writer().println("Usage: printAbove [start|stop]")
                            }
                        } else {
                            terminal.writer().println("Usage: printAbove [start|stop]")
                        }
                    } else if ("set" == pl.word()) {
                        if (pl.words().size == 3) {
                            reader.setVariable(pl.words()[1], pl.words()[2])
                        } else {
                            terminal.writer().println("Usage: set <name> <value>")
                        }
                    } else if ("tput" == pl.word()) {
                        if (pl.words().size == 2) {
                            val vcap = InfoCmp.Capability.byName(pl.words()[1])
                            if (vcap != null) {
                                terminal.puts(vcap)
                            } else {
                                terminal.writer().println("Unknown capability")
                            }
                        } else {
                            terminal.writer().println("Usage: tput <capability>")
                        }
                    } else if ("testkey" == pl.word()) {
                        terminal.writer().write("Input the key event(Enter to complete): ")
                        terminal.writer().flush()
                        val sb = StringBuilder()
                        while (true) {
                            val c = (reader as LineReaderImpl).readCharacter()
                            if (c == 10 || c == 13) break
                            sb.append(String(Character.toChars(c)))
                        }
                        terminal.writer().println(KeyMap.display(sb.toString()))
                        terminal.writer().flush()
                    } else if ("cls" == pl.word()) {
                        terminal.puts(InfoCmp.Capability.clear_screen)
                        terminal.flush()
                    } else if ("sleep" == pl.word()) {
                        Thread.sleep(3000)
                    } else if ("nano" == pl.word()) {
                        Commands.nano(
                            terminal, System.out, System.err,
                            Paths.get(""),
                            argv
                        )
                    } else if ("less" == pl.word()) {
                        Commands.less(
                            terminal, System.`in`, System.out, System.err,
                            Paths.get(""),
                            argv
                        )
                    } else if ("history" == pl.word()) {
                        Commands.history(reader, System.out, System.err, Paths.get(""), argv)
                    } else if ("setopt" == pl.word()) {
                        Commands.setopt(reader, System.out, System.err, argv)
                    } else if ("unsetopt" == pl.word()) {
                        Commands.unsetopt(reader, System.out, System.err, argv)
                    } else if ("ttop" == pl.word()) {
                        TTop.ttop(terminal, System.out, System.err, argv)
                    } else if ("help" == pl.word() || "?" == pl.word()) {
                        help()
                    } else if ("select" == pl.word()) {
                        val selector = OptionSelector(
                            terminal, "Select number>", Arrays.asList("one", "two", "three", "four")
                        )
                        val selected = selector.select()
                        println("You selected number $selected")
                    }
                } catch (e: HelpException) {
                    HelpException.highlight(e.message, HelpException.defaultStyle()).print(terminal)
                } catch (e: IllegalArgumentException) {
                    println(e.message)
                } catch (e: UserInterruptException) {
                    // Ignore
                } catch (e: EndOfFileException) {
                    return
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private class OptionSelector(private val terminal: Terminal, title: String, options: Collection<String>?) {
        private enum class Operation {
            FORWARD_ONE_LINE, BACKWARD_ONE_LINE, EXIT
        }

        private val lines: MutableList<String> = ArrayList()
        private val size = Size()
        private val bindingReader: BindingReader

        init {
            bindingReader = BindingReader(terminal.reader())
            lines.add(title)
            lines.addAll(options!!)
        }

        private fun displayLines(cursorRow: Int): List<AttributedString> {
            val out: MutableList<AttributedString> = ArrayList()
            var i = 0
            for (s in lines) {
                if (i == cursorRow) {
                    out.add(AttributedStringBuilder().append(s, AttributedStyle.INVERSE).toAttributedString())
                } else {
                    out.add(AttributedString(s))
                }
                i++
            }
            return out
        }

        private fun bindKeys(map: KeyMap<Operation>) {
            map.bind(
                Operation.FORWARD_ONE_LINE, "e", KeyMap.ctrl('E'), KeyMap.key(
                    terminal, InfoCmp.Capability.key_down
                )
            )
            map.bind(
                Operation.BACKWARD_ONE_LINE, "y", KeyMap.ctrl('Y'), KeyMap.key(
                    terminal, InfoCmp.Capability.key_up
                )
            )
            map.bind(Operation.EXIT, "\r")
        }

        fun select(): String {
            val display = Display(terminal, true)
            val attr = terminal.enterRawMode()
            try {
                terminal.puts(InfoCmp.Capability.enter_ca_mode)
                terminal.puts(InfoCmp.Capability.keypad_xmit)
                terminal.writer().flush()
                size.copy(terminal.size)
                display.clear()
                display.reset()
                var selectRow = 1
                val keyMap = KeyMap<Operation>()
                bindKeys(keyMap)
                while (true) {
                    display.resize(size.rows, size.columns)
                    display.update(displayLines(selectRow), size.cursorPos(0, lines[0].length))
                    val op = bindingReader.readBinding(keyMap)
                    when (op) {
                        Operation.FORWARD_ONE_LINE -> {
                            selectRow++
                            if (selectRow > lines.size - 1) {
                                selectRow = 1
                            }
                        }

                        Operation.BACKWARD_ONE_LINE -> {
                            selectRow--
                            if (selectRow < 1) {
                                selectRow = lines.size - 1
                            }
                        }

                        Operation.EXIT -> return lines[selectRow]
                    }
                }
            } finally {
                terminal.attributes = attr
                terminal.puts(InfoCmp.Capability.exit_ca_mode)
                terminal.puts(InfoCmp.Capability.keypad_local)
                terminal.writer().flush()
            }
        }
    }
}