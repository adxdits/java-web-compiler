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

 async function runCode() {
   try {
     const code = editorInstanceRef.current.getValue();
     const response = await fetch('/run', { method: 'POST' });
     if (!response.ok) throw new Error(response.statusText);
     const data = await response.json();
     setOutput(data.output);
   } catch (err) {
     console.error('Run failed:', err);
   }
 }

  async function compileCode() {
    if (!editorInstanceRef.current || !monacoRef.current) return;

    const currentCode = editorInstanceRef.current.getValue();
    const monaco = monacoRef.current;
    const model = editorInstanceRef.current.getModel();

    try {
      const result = await fetchResponse(currentCode);
      setClassName(result.className);
      const errors = result.diagnostics;

      if (errors.length === 0) {
        alert("Compilation successful!");
        monaco.editor.setModelMarkers(model, 'java-compiler', []);
        return;
      }

      // Map Java diagnostics to Monaco markers
      const markers = errors.map(err => ({
        severity: monaco.MarkerSeverity.Error,
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

  async function downloadCode() {
    if (!editorInstanceRef.current){ return; } 
    const code = editorInstanceRef.current.getValue();
    let name = className;
    try {
      const response = await fetch('/className', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code })
      });
      if (response.ok) {
        const data = await response.json();
        name = data.className;
        setClassName(name);
      }
    } catch (err) {
      // fall through with current className
    }
    const blob = new Blob([code], { type: 'text/x-java-source' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${name}.java`;
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <div style={{ padding: '10px', backgroundColor: '#202124', borderBottom: '1px solid #333', display: 'flex', gap: '10px' }}>
        <button
          onClick={compileCode}
          style={{ padding: '8px 16px', fontSize: '14px', cursor: 'pointer', backgroundColor: '#4CAF50', color: 'white', border: 'none', borderRadius: '4px' }}>
          Compile Code
        </button>
        <button
          onClick={runCode}
          style={{ padding: '8px 16px', fontSize: '14px', cursor: 'pointer', backgroundColor: '#FF9800', color: 'white', border: 'none', borderRadius: '4px' }}>
          Run
        </button>
        <button
          onClick={downloadCode}
          style={{ padding: '8px 16px', fontSize: '14px', cursor: 'pointer', backgroundColor: '#2196F3', color: 'white', border: 'none', borderRadius: '4px' }}>
          Download
        </button>
      </div>
      {/* The div where Monaco will inject itself */}
      <div ref={editorDivRef} style={{ flexGrow: 1 }} />
      <div style={{ height: '150px', backgroundColor: '#1e1e1e', borderTop: '2px solid #4CAF50', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '4px 10px', backgroundColor: '#2d2d2d', color: '#888', fontSize: '12px' }}>Output</div>
        <pre style={{ margin: 0, padding: '10px', color: '#d4d4d4', fontFamily: 'monospace', overflowY: 'auto', flexGrow: 1 }}>
          {output ?? 'Run your code to see output here...'}
        </pre>
      </div>
    </div>
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<JavaEditor />);