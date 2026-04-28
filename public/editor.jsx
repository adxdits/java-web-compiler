const { useState, useRef, useEffect } = React;

function JavaEditor() {
  const editorDivRef = useRef(null);
  const editorInstanceRef = useRef(null);
  const monacoRef = useRef(null);
  const [output, setOutput] = useState(null);
  const [className, setClassName] = useState('Main');

  const defaultCode = 'public class Main {\n  public static void main(String[] args) {\n    System.out.println("Hello World");\n  }\n}';

  useEffect(() => {
    // Configure the AMD loader to point to the Monaco CDN
    window.require.config({ paths: { 'vs': 'https://unpkg.com/monaco-editor@0.47.0/min/vs' }});

    // Load the main editor module
    window.require(['vs/editor/editor.main'], () => {
      const monaco = window.monaco;
      monacoRef.current = monaco;

      // Create the editor instance and attach it to the div
      editorInstanceRef.current = monaco.editor.create(editorDivRef.current, {
        value: defaultCode,
        language: 'java',
        theme: 'vs-dark',
        minimap: { enabled: false }
      });
    });

    // Cleanup function to destroy the editor when the component unmounts
    return () => {
      if (editorInstanceRef.current) {
        editorInstanceRef.current.dispose();
      }
    };
  }, []);

   async function fetchResponse(code) {
     const response = await fetch('/compile', {
       method: 'POST',
       headers: {
         'Content-Type': 'application/json'
       },
       body: JSON.stringify({ code })
     });

     if (!response.ok) {
       throw new Error(response.statusText);
     }

     return response.json();
   }

  async function fetchRun(currentCode) {
   const response = await fetch('/run', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ code: currentCode })
    });

    if (!response.ok) throw new Error(response.statusText);
    return response.json();
  }

  async function runCode() {
    if (!editorInstanceRef.current || !monacoRef.current) return;

    const currentCode = editorInstanceRef.current.getValue();
    const monaco = monacoRef.current;
    const model = editorInstanceRef.current.getModel();

    try {
      const data = await fetchRun(currentCode);

      if (data.errors.length === 0) {
        monaco.editor.setModelMarkers(model, 'java-compiler', []);
      }

      // Map Java diagnostics to Monaco markers
      const markers = data.errors.map(err => ({
        severity: monaco.MarkerSeverity.Error,
        startLineNumber: err.line,
        startColumn: err.column,
        endLineNumber: err.line,
        endColumn: err.column + 1, // Highlight the character token
        message: err.message
      }));

      // Apply red squiggles
      monaco.editor.setModelMarkers(model, 'java-compiler', markers);
      setOutput(data.output);
    } catch (err) {
      console.error('Compilation fetch failed', err);
    }
  }

  async function compileCode() {
    if (!editorInstanceRef.current || !monacoRef.current) return;

    const currentCode = editorInstanceRef.current.getValue();
    const monaco = monacoRef.current;
    const model = editorInstanceRef.current.getModel();

    try {
      const data = await fetchResponse(currentCode);
      const errors = data.diagnostics || [];
      console.log(data);
      setClassName(data.className);

      if (errors.length === 0) {
        alert("Compilation successful!");
        monaco.editor.setModelMarkers(model, 'java-compiler', []);
        return;
      }

      setOutput(errors.map(e => `[${e.kind}] Ligne ${e.line}, col ${e.column} — ${e.message}`).join('\n'));

      // Map Java diagnostics to Monaco markers
      const markers = errors.map(err => ({
        severity: err.kind === 'ERROR' ? monaco.MarkerSeverity.Error : monaco.MarkerSeverity.Warning,
        startLineNumber: err.line,
        startColumn: err.column,
        endLineNumber: err.line,
        endColumn: err.column + 1, // Highlight the character token
        message: err.message
      }));

      // Apply red squiggles
      monaco.editor.setModelMarkers(model, 'java-compiler', markers);

    } catch (err) {
      console.error('Compilation fetch failed', err);
    }
  }

  function downloadCode() {
    if (!editorInstanceRef.current){ return; } 
    const code = editorInstanceRef.current.getValue();
    const blob = new Blob([code], { type: 'text/x-java-source' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${className}.java`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div className="app-container">
      <div className="toolbar">
        <button onClick={compileCode} className="btn-compile">
          Compile Code
        </button>
        <button onClick={runCode} className="btn-run">
          Run
        </button>
        <button onClick={downloadCode} className="btn-download">
          Download
        </button>
      </div>
      {/* The div where Monaco will inject itself */}
      <div ref={editorDivRef} className="editor-container" />
      <div className="output-panel">
        <div className="output-header">Output</div>
        <pre className="output-content">
          {output ?? 'Run your code to see output here...'}
        </pre>
      </div>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<JavaEditor />);