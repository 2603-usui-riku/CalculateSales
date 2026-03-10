package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";
	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";
	// 賞品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "が存在しません";
	private static final String INVALID_FORMAT = "のフォーマットが不正です";
	private static final String INVALID_BRANCH_CODE = "の支店コードが不正です";
	private static final String SALES_FILE_NAME_NOT_SERIAL = "売上ファイル名が連番になっていません";
	private static final String TOTAL_AMOUNT_OVER_LIMIT = "合計金額が10桁を超えました";

	// 正規表現
	private static final String BRANCH_CODE_REGAX = "^\\d{3}$";
	private static final String COMMODITY_CODE_REGAX = "^[a-zA-Z0-9]{8}$";
	private static final String SALES_FILE_NAME_REGAX = "^\\d{8}\\.rcd$";
	private static final String NUMBER_REGAX = "^\\d+$";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			// コマンドライン引数が1つ設定されていなかった場合は、
			// エラーメッセージをコンソールに表示します。
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales, "支店定義ファイル", BRANCH_CODE_REGAX)) {
			return;
		}

		// 商品定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_COMMODITY_LST, commodityNames, commoditySales, "商品定義ファイル", COMMODITY_CODE_REGAX)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<File>();

		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile() && files[i].getName().matches(SALES_FILE_NAME_REGAX)) {
				// 対象がファイルであり、「数字8桁.rcd」なのか判定します。
				rcdFiles.add(files[i]);
			}
		}

		Collections.sort(rcdFiles);
		// 比較回数は売上ファイルの数よりも1回少ないため、
		// 繰り返し回数は売上ファイルのリストの数よりも1つ小さい数です。
		for (int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			// 比較する2つのファイル名の先頭から数字の8文字を切り出し、int型に変換します。
			if ((latter - former) != 1) {
				// 2つのファイル名の数字を比較して、差が1ではなかったら、
				// エラーメッセージをコンソールに表示します。
				System.out.println(SALES_FILE_NAME_NOT_SERIAL);
				return;
			}
		}

		// rcdFilesに複数の売上ファイルの情報を格納しているので、その数だけ繰り返します。
		for (int i = 0; i < rcdFiles.size(); i++) {
			ArrayList<String> contents = new ArrayList<String>();
			String saleFileName = rcdFiles.get(i).getName();

			BufferedReader br = null;
			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);

				String line;
				while ((line = br.readLine()) != null) {
					contents.add(line);
				}
				if (contents.size() != 3) {
					// 売上ファイルの行数が3行ではなかった場合は、
					// エラーメッセージをコンソールに表示します。
					System.out.println(saleFileName + INVALID_FORMAT);
					return;
				}

				String branchCode = contents.get(0);
				if (!branchNames.containsKey(branchCode)) {
					// 支店情報を保持しているMapに売上ファイルの支店コードが存在しなかった場合は、
					// エラーメッセージをコンソールに表示します。
					System.out.println(saleFileName + INVALID_BRANCH_CODE);
					return;
				}

				String commodityCode = contents.get(1);
				if (!commodityNames.containsKey(commodityCode)) {
					// 商品情報を保持しているMapに売上ファイルの商品コードが存在しなかった場合は、
					// エラーメッセージをコンソールに表示します。
					System.out.println(saleFileName + INVALID_BRANCH_CODE);
					return;
				}

				// 売上ファイルから読み込んだ売上金額をMapに加算していくために、型の変換を行います。
				// ※詳細は後述で説明
				String fileSale = contents.get(2);
				if (!fileSale.matches(NUMBER_REGAX)) {
					// 売上金額が数字ではなかった場合は、
					// エラーメッセージをコンソールに表示します。
					System.out.println(UNKNOWN_ERROR);
					return;
				}
				long fileSaleAmount = Long.parseLong(fileSale);

				// 読み込んだ売上金額を加算します。
				// ※詳細は後述で説明
				Long totalBranchSalesAmount = branchSales.get(branchCode) + fileSaleAmount;
				Long totalCommoditySalesAmount = commoditySales.get(commodityCode) + fileSaleAmount;
				if ((totalBranchSalesAmount >= 10000000000L) || (totalCommoditySalesAmount >= 10000000000L)) {
					System.out.println(TOTAL_AMOUNT_OVER_LIMIT);
					return;
				}

				// 加算した売上金額をMapに追加します。
				branchSales.put(branchCode, totalBranchSalesAmount);
				commoditySales.put(commodityCode, totalCommoditySalesAmount);
			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		// 商品別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 識別コードと名称を保持するMap
	 * @param 識別コードと売上金額を保持するMap
	 * @param エラーメッセージを保持する配列
	 * @param 正規表現
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> namesMap,
			Map<String, Long> salesMap, String errFileName, String regax) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			if (!file.exists()) {
				//ファイルが存在しない場合、コンソールにエラーメッセージを表示します。
				System.out.println(errFileName + FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] lines = line.split(",");

				if ((lines.length != 2) || (!lines[0].matches(regax))) {
					// 定義ファイルの仕様が満たされていない場合、
					// エラーメッセージをコンソールに表示します。
					System.out.println(errFileName + INVALID_FORMAT);
					return false;
				}

				namesMap.put(lines[0], lines[1]);
				salesMap.put(lines[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 識別コードと名称を保持するMap
	 * @param 識別コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> namesMap,
			Map<String, Long> salesMap) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : namesMap.keySet()) {
				bw.write(key + "," + namesMap.get(key) + "," + salesMap.get(key));
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
