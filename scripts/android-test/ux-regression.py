"""Functional emulator scenarios. See README.md here; never a physical performance benchmark."""
from pathlib import Path
import importlib.util,time,json,re,subprocess,sqlite3,sys,hashlib,traceback,tempfile
from contextlib import contextmanager
from PIL import Image
repo=Path(__file__).resolve().parents[2];spec=importlib.util.spec_from_file_location('u',repo/'scripts/emulator-ui.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
u=m.EmulatorUI('emulator-5580');report=repo/'docs/acceptance/reports/UX-MVP-6-functional.json'
W,H=800,1200
def output(key,value):
 data=json.loads(report.read_text()) if report.exists() else {'scope':'Release Android36 x86_64 emulator; not Huawei acceptance','physicalDeviceValidated':False}
 data[key]=value;report.write_text(json.dumps(data,indent=2),encoding='utf-8');print(key,json.dumps(value),flush=True)
@contextmanager
def db():
 raw=subprocess.run([str(u.adb),'-s',u.serial,'exec-out','su','0','cat','/data/data/com.kitap/databases/kitap.db'],capture_output=True,check=True,timeout=45).stdout
 temp=tempfile.NamedTemporaryFile(dir=u.private,prefix='ux6-db-',suffix='.sqlite',delete=False);file=Path(temp.name);temp.write(raw);temp.close();conn=sqlite3.connect(file)
 try:
  assert conn.execute('PRAGMA quick_check').fetchone()[0]=='ok'
  yield conn
 finally:conn.close();file.unlink(missing_ok=True)
def counts():
 with db() as c:return {t:c.execute('SELECT COUNT(*) FROM '+t).fetchone()[0] for t in ['documents','annotations','notes','note_drafts','bookmarks','thumbnail_cache']}
def sync_dimensions():
 global W,H
 with Image.open(u.screenshot('ux6-current-geometry.png')) as image:W,H=image.size
def restart():
 u.run('shell','cmd','statusbar','collapse');u.run('shell','am','force-stop','com.kitap');u.run('shell','am','start','-n','com.kitap/.MainActivity');u.wait('Kitaplığım',timeout=45);sync_dimensions()
def ime():
 state=u.run('shell','dumpsys','input_method')
 if re.search(r'mInputShown=true|isInputViewShown=true',state):u.run('shell','input','keyevent','4')
def top():
 for _ in range(8):
  if u.find('Kitap adında ara') is not None:return
  u.run('shell','input','swipe',str(W//2),str(H//3),str(W//2),str(H*4//5),'650')
 raise AssertionError('Library search not reachable')
def replace(value):
 u.run('shell','input','keycombination','113','29');u.run('shell','input','keyevent','67')
 if value:u.run('shell','input','text',value)
def search(value):
 top();u.click('Kitap adında ara');replace(value);ime();u.root()
def metadata(title):
 with db() as c:return c.execute('SELECT d.id,p.page_index,d.page_count FROM documents d JOIN reading_positions p ON p.document_id=d.id WHERE d.title=?',(title,)).fetchone()
def open_book(title):
 item=metadata(title);assert item is not None
 search(title if title.isascii() else '')
 label=f'{title}, {item[1]+1} / {item[2]}. Okumaya devam et'
 for _ in range(12):
  node=u.find(label)
  bounds=list(map(int,re.findall(r'\d+',node.get('bounds')))) if node is not None else []
  if len(bounds)==4 and bounds[3]>bounds[1] and 0<=(bounds[1]+bounds[3])//2<H-48:break
  u.run('shell','input','swipe',str(W//2),str(H*65//100),str(W//2),str(H//5),'650')
 u.click(label);u.wait(f'{title}, PDF sayfası {item[1]+1}',timeout=60);sync_dimensions()
 return item
def leave_reader():
 for _ in range(7):
  if u.find('Kitaplığım') is not None:return
  u.run('shell','input','keyevent','4');time.sleep(.4)
 raise AssertionError('Reader did not close')
def overview():
 if u.find('Paneli kapat') is not None:return
 if u.find('Sayfalar') is None:u.run('shell','input','tap',str(W//2),str(H//2))
 u.wait('Sayfalar');u.click('Sayfalar');u.wait('Paneli kapat')
def jump(title,page):
 overview();u.click('Sayfa numarası');replace(str(page));u.click('Git');u.wait(f'{title}, PDF sayfası {page}',timeout=60)
def picker_search(name):
 if u.find('Search') is not None:u.click('Search')
 if u.find('Clear query') is not None:u.click('Clear query')
 u.run('shell','input','text',Path(name).stem[:32]);u.click(name,timeout=45)
def import_pdf(name):
 search('');u.click('PDF ekle');picker_search(name);u.wait('Kitaplığım',timeout=60)
def touch(*values):return u.run('shell','CLASSPATH=/data/local/tmp/kitap-touch.jar','app_process','/system/bin','KitapTouch',*map(str,values))
def check_grid(name,page):
 overview();root=u.root();first=(page-1)//3*3+1
 cells=[]
 for number in range(first,first+3):
  n=u.find(f'Sayfa {number}',root=root);assert n is not None;cells.append(list(map(int,re.findall(r'\d+',n.get('bounds')))))
 assert len(set(c[0] for c in cells))==3
 path=u.screenshot(name);im=Image.open(path).convert('RGB');y=cells[0][1]+20;x=(cells[0][2]+cells[1][0])//2
 assert max(abs(a-b) for a,b in zip(im.getpixel((x,y)),(30,33,36)))<8,'Native thumbnail painted into the gutter'
 for left,top_,right,bottom in cells:
  area=im.crop((left+5,max(top_,bottom-52),right-5,bottom-4));bright=sum(1 for c in area.getdata() if min(c)>130 and max(c)-min(c)<50)
  assert bright>10,'Native thumbnail covered the physical page label'
 return {'columns':3,'gutterAndPageNumbersVisible':True}
def basic():
 restart();open_book('UX-80.pdf');jump('UX-80.pdf',45)
 result=check_grid('ux6-overview.png',45);u.click('Paneli kapat');u.wait('UX-80.pdf, PDF sayfası 45')
 u.screenshot('ux6-reader.png');u.run('shell','input','tap','400','650');u.wait('Sayfaya not ekle');u.screenshot('ux6-overlay.png')
 for label in ['Kitaba not ekle','Metin seç / Altını çiz','Sayfalar']:assert u.find(label) is not None
 for label in ['Büyüt','Gece','Notlar','Altı çizilenler']:assert u.find(label) is None
 leave_reader();search('UX-');u.screenshot('ux6-library-synthetic.png');output('basic',result)
def long_pdf():
 restart()
 if metadata('UX-1000.pdf') is None:import_pdf('UX-1000.pdf')
 assert metadata('UX-1000.pdf')[2]==1000
 open_book('UX-1000.pdf');jump('UX-1000.pdf',500);before=counts()['thumbnail_cache'];check_grid('ux6-thousand-pages.png',500)
 first_range=[n.get('content-desc') for n in u.root().iter('node') if re.fullmatch(r'Sayfa \d+',n.get('content-desc',''))]
 u.run('shell','input','swipe',str(W//2),str(H*85//100),str(W//2),str(H*40//100),'800');second_range=[n.get('content-desc') for n in u.root().iter('node') if re.fullmatch(r'Sayfa \d+',n.get('content-desc',''))];assert first_range!=second_range;u.click('Paneli kapat');u.wait('UX-1000.pdf, PDF sayfası 500');leave_reader()
 after=counts()['thumbnail_cache'];assert after-before<100
 output('longPdf',{'pages':1000,'resumedPhysicalPage':metadata('UX-1000.pdf')[1]+1,'thumbnailCacheEntryIncrease':after-before,'overviewScrollDoesNotSave':metadata('UX-1000.pdf')[1]==499})
def scan():
 restart()
 if metadata('UX-scan.pdf') is None:import_pdf('UX-scan.pdf')
 open_book('UX-scan.pdf');u.run('shell','input','tap','400','600');u.wait('Metin seç / Altını çiz');u.click('Metin seç / Altını çiz');u.wait('Seçimi iptal et');u.run('shell','input','tap','350','500');u.wait('Bölgeyi kaydet',timeout=45)
 u.click('Bölge açıklaması');replace('UX_scan_region');ime();u.click('Bölgeyi kaydet');u.wait('İşaret kaydedildi.');leave_reader()
 with db() as c:
  row=c.execute("SELECT kind,provenance FROM annotations WHERE document_id=? AND deleted=0 ORDER BY created_at DESC LIMIT 1",(metadata('UX-scan.pdf')[0],)).fetchone();assert row==('region','manual-region')
 output('scan',{'doesNotPretendTextSelection':True,'regionCommitted':True})
def layouts_pdf():
 restart()
 if metadata('UX-layouts.pdf') is None:import_pdf('UX-layouts.pdf')
 open_book('UX-layouts.pdf')
 for page in [1,2,3]:
  jump('UX-layouts.pdf',page);u.screenshot(f'ux6-layout-{page}.png')
 leave_reader();before=counts()['documents'];import_pdf('UX-broken.pdf');assert counts()['documents']==before
 output('layoutCorpus',{'wideMultiColumnRotatedPagesOpened':3,'brokenImportDidNotCreateBook':True})
def corpus():
 restart()
 with db() as c:items=c.execute("SELECT title,page_count FROM documents WHERE title NOT LIKE 'UX-%' ORDER BY id").fetchall()
 done=[]
 for index,(title,count) in enumerate(items,1):
  restart();item=open_book(title);target=min(count,item[1]+2);jump(title,target);leave_reader();assert metadata(title)[1]+1==target
  done.append({'alias':f'DL{index:02d}','pageCount':count,'displayedAndSaved':target});output('downloadsCorpus',done)
def memory():
 value=u.run('shell','dumpsys','meminfo','com.kitap');match=re.search(r'TOTAL PSS:\s*([\d,]+)',value)
 (u.private/'ux6-meminfo.txt').write_text(value)
 output('memorySnapshot',{'pssKiB':int(match.group(1).replace(',','')) if match else None,'emulatorOnly':True,'notPhysicalAcceptance':True})
def responsive():
 global W,H
 fixture='UX-Uzun-Kitap-Basligi-Telefon-ve-Tablet-Icin-Okuma-Deneyimi-Uzun-Baslik-Testi.pdf'
 restart()
 if metadata(fixture) is None:import_pdf(fixture)
 originalFont=u.run('shell','settings','get','system','font_scale').strip()
 old_rotation=u.run('shell','settings','get','system','user_rotation').strip();old_auto=u.run('shell','settings','get','system','accelerometer_rotation').strip()
 u.run('shell','settings','put','system','accelerometer_rotation','0');u.run('shell','settings','put','system','user_rotation','0')
 results=[]
 try:
  for width,height in [(360,800),(600,960),(900,800),(1280,800)]:
   u.run('shell','wm','size',f'{width}x{height}');W,H=width,height;restart();search('UX-');root=u.root();cards=[n for n in root.iter('node') if n.get('content-desc','').startswith('UX-') and 'Okumaya devam et' in n.get('content-desc','')];xs=set(int(re.findall(r'\d+',n.get('bounds'))[0]) for n in cards)
   expected=2 if width<600 else 3 if width<840 else 4 if width<1200 else 5
   assert len(xs)==expected,(width,len(xs));u.screenshot(f'ux6-library-{width}.png');results.append({'widthDp':width,'columnsObserved':len(xs),'columnPolicy':expected})
  u.run('shell','wm','size','360x480');W,H=360,480;u.run('shell','settings','put','system','font_scale','2.0');restart();open_book('UX-80.pdf');overview();root=u.root();assert u.find('Paneli kapat',root=root) is not None
  cells=[n for n in root.iter('node') if re.fullmatch(r'Sayfa \d+',n.get('content-desc',''))];assert cells and any(int(re.findall(r'\d+',n.get('bounds'))[3])-int(re.findall(r'\d+',n.get('bounds'))[1])>80 for n in cells)
  u.screenshot('ux6-font200-phone.png');u.click('Paneli kapat');leave_reader();results.append({'phone360x480FontPercent':200,'closeAndGridReachable':True})
 finally:
  u.run('shell','settings','put','system','font_scale',originalFont if originalFont not in ['null',''] else '1.0');u.run('shell','wm','size','800x1200');u.run('shell','settings','put','system','user_rotation',old_rotation if old_rotation!='null' else '0');u.run('shell','settings','put','system','accelerometer_rotation',old_auto if old_auto!='null' else '0');W,H=800,1200
 output('responsive',results)
def run(name):
 try:globals()[name]()
 except Exception:
  (u.private/f'ux6-{name}-failure.txt').write_text(traceback.format_exc(),encoding='utf-8');output(name,{'passed':False,'details':'Private test diagnostic retained'});raise
if __name__=='__main__':
 for case in sys.argv[1:]:run(case)
