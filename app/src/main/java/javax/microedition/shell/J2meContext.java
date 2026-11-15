/*
 * Copyright 2025 Yury Kharchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package javax.microedition.shell;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.event.EventQueue;
import javax.microedition.lcdui.keyboard.VirtualKeyboard;

/**
 * A running MIDlet session. Each launched J2ME application is one context,
 * owning its own class loader, display, event queue, midlet thread and
 * virtual keyboard. A static registry keeps every live context so the UI
 * (tabs / floating window) can switch between them.
 */
public class J2meContext {
	private static final InheritableThreadLocal<J2meContext> currentContext = new InheritableThreadLocal<>();
	private static final Map<String, J2meContext> contexts = new ConcurrentHashMap<>();

	private final String name;
	private AppClassLoader classLoader;
	private Display display;
	private EventQueue eventQueue;
	private MidletThread midletThread;
	private VirtualKeyboard vk;
	private UiHandler uiHandler;

	public interface UiHandler {
		void onDisplayableChanged(Displayable displayable);

		void onMidletExit(J2meContext context);

		void showError(String message);
	}

	public J2meContext(String name) {
		this.name = name;
		EventQueue queue = new EventQueue();
		this.eventQueue = queue;
		queue.startProcessing();
		contexts.put(name, this);
	}

	public static J2meContext current() {
		return currentContext.get();
	}

	public static J2meContext get(String name) {
		return contexts.get(name);
	}

	public static Collection<J2meContext> getAllContexts() {
		return contexts.values();
	}

	public String getName() {
		return name;
	}

	public AppClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(AppClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public Display getDisplay() {
		return display;
	}

	public void setDisplay(Display display) {
		this.display = display;
	}

	public EventQueue getEventQueue() {
		return eventQueue;
	}

	public MidletThread getMidletThread() {
		return midletThread;
	}

	public void setMidletThread(MidletThread midletThread) {
		this.midletThread = midletThread;
	}

	public VirtualKeyboard getVk() {
		return vk;
	}

	public void setVk(VirtualKeyboard vk) {
		this.vk = vk;
	}

	public UiHandler getUiHandler() {
		return uiHandler;
	}

	public void setUiHandler(UiHandler uiHandler) {
		this.uiHandler = uiHandler;
	}

	public void register() {
		currentContext.set(this);
	}

	public void unregister() {
		currentContext.remove();
		contexts.remove(name);
	}
}
